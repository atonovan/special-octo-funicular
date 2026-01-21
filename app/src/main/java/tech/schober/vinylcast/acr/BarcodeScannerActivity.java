package tech.schober.vinylcast.acr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tech.schober.vinylcast.R;
import tech.schober.vinylcast.acr.api.AudioRecognitionApiClient;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import timber.log.Timber;

/**
 * Activity for scanning barcodes on vinyl albums
 * Uses CameraX + ML Kit for barcode detection
 */
public class BarcodeScannerActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    private PreviewView cameraPreview;
    private TextView instructionText;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private AudioRecognitionApiClient apiClient;
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scanner);

        cameraPreview = findViewById(R.id.cameraPreview);
        instructionText = findViewById(R.id.instructionText);

        cameraExecutor = Executors.newSingleThreadExecutor();
        apiClient = new AudioRecognitionApiClient();

        // Initialize barcode scanner with UPC/EAN formats
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{CAMERA_PERMISSION},
                    CAMERA_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required for barcode scanning",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Timber.e(e, "Error starting camera");
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        // Preview
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

        // Image analysis for barcode scanning
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeBarcodes);

        // Select back camera
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        // Bind to lifecycle
        cameraProvider.unbindAll();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeBarcodes(@NonNull ImageProxy imageProxy) {
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(mediaImage,
                imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        if (barcode.getFormat() == Barcode.FORMAT_UPC_A ||
                                barcode.getFormat() == Barcode.FORMAT_UPC_E ||
                                barcode.getFormat() == Barcode.FORMAT_EAN_13 ||
                                barcode.getFormat() == Barcode.FORMAT_EAN_8) {
                            String barcodeValue = barcode.getRawValue();
                            if (barcodeValue != null && !barcodeValue.isEmpty()) {
                                onBarcodeDetected(barcodeValue);
                                break;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Timber.e(e, "Barcode scanning failed"))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void onBarcodeDetected(String barcode) {
        if (isProcessing) {
            return;
        }

        isProcessing = true;
        Timber.i("Barcode detected: %s", barcode);

        runOnUiThread(() -> instructionText.setText("Processing: " + barcode));

        // Look up barcode in background thread
        new Thread(() -> {
            RecognitionResult result = apiClient.recognizeFromBarcode(barcode);

            runOnUiThread(() -> {
                if (result != null) {
                    Timber.i("Album found: %s - %s", result.getArtist(), result.getAlbum());
                    Toast.makeText(this,
                            result.getArtist() + " - " + result.getAlbum(),
                            Toast.LENGTH_LONG).show();
                    // TODO: Return result to calling activity or show details
                    finish();
                } else {
                    Toast.makeText(this, "Album not found in database", Toast.LENGTH_SHORT).show();
                    instructionText.setText("Align barcode within frame");
                    isProcessing = false;
                }
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        barcodeScanner.close();
    }
}
