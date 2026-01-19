package tech.schober.vinylcast.acr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tech.schober.vinylcast.acr.api.AudioRecognitionApiClient;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import tech.schober.vinylcast.audio.AudioStreamProvider;
import tech.schober.vinylcast.audio.NativeAudioEngine;
import timber.log.Timber;

/**
 * Stream provider that performs audio recognition on the audio stream
 */
public class AudioRecognitionStreamProvider implements Runnable {
    private static final int RECOGNITION_INTERVAL_MS = 30000; // Recognize every 30 seconds
    private static final int FINGERPRINT_DURATION_SEC = 15; // Use 15 seconds of audio for fingerprinting
    private static final int BUFFER_SIZE = 4096;

    private final AudioStreamProvider audioStreamProvider;
    private final int sampleRate;
    private final int channelCount;
    private final CopyOnWriteArrayList<AudioRecognitionListener> listeners;
    private final AudioRecognitionApiClient apiClient;
    private final ExecutorService executor;

    private Thread thread;
    private volatile boolean running = false;
    private long lastRecognitionTime = 0;

    public AudioRecognitionStreamProvider(AudioStreamProvider audioStreamProvider,
                                          int sampleRate,
                                          int channelCount) {
        this.audioStreamProvider = audioStreamProvider;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.listeners = new CopyOnWriteArrayList<>();
        this.apiClient = new AudioRecognitionApiClient();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void addListener(AudioRecognitionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(AudioRecognitionListener listener) {
        listeners.remove(listener);
    }

    public void start() {
        if (running) {
            Timber.w("AudioRecognitionStreamProvider already running");
            return;
        }

        running = true;
        thread = new Thread(this, "AudioRecognitionThread");
        thread.start();
        Timber.i("AudioRecognitionStreamProvider started");
    }

    public void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
                Timber.e(e, "Failed to join AudioRecognitionThread");
            }
            thread = null;
        }
        executor.shutdown();
        Timber.i("AudioRecognitionStreamProvider stopped");
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        try {
            inputStream = audioStreamProvider.getAudioInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int samplesNeeded = sampleRate * channelCount * FINGERPRINT_DURATION_SEC;
            short[] fingerprintBuffer = new short[samplesNeeded];
            int fingerprintSampleCount = 0;

            while (running) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead <= 0) {
                    Thread.sleep(10);
                    continue;
                }

                // Convert bytes to shorts (16-bit PCM)
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                while (byteBuffer.hasRemaining() && fingerprintSampleCount < samplesNeeded) {
                    fingerprintBuffer[fingerprintSampleCount++] = byteBuffer.getShort();
                }

                // Check if we have enough samples and enough time has passed
                long currentTime = System.currentTimeMillis();
                if (fingerprintSampleCount >= samplesNeeded &&
                        (currentTime - lastRecognitionTime) >= RECOGNITION_INTERVAL_MS) {

                    lastRecognitionTime = currentTime;
                    final short[] samplesForRecognition = fingerprintBuffer.clone();

                    // Perform recognition on a background thread
                    executor.execute(() -> performRecognition(samplesForRecognition));

                    // Reset for next recognition
                    fingerprintSampleCount = 0;
                }
            }
        } catch (IOException | InterruptedException e) {
            Timber.e(e, "Error in AudioRecognitionStreamProvider");
            notifyRecognitionFailed(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Timber.e(e, "Failed to close input stream");
                }
            }
        }
    }

    private void performRecognition(short[] samples) {
        try {
            Timber.i("Starting audio recognition...");
            notifyRecognitionInProgress();

            // Create chromaprint context
            long chromaprintCtx = NativeAudioEngine.createChromaprint(sampleRate, channelCount);
            if (chromaprintCtx == 0) {
                Timber.e("Failed to create chromaprint context");
                notifyRecognitionFailed("Failed to create fingerprint");
                return;
            }

            try {
                // Feed audio samples to chromaprint
                if (!NativeAudioEngine.feedChromaprint(chromaprintCtx, samples, samples.length)) {
                    Timber.e("Failed to feed chromaprint");
                    notifyRecognitionFailed("Failed to process audio");
                    return;
                }

                // Get fingerprint
                String fingerprint = NativeAudioEngine.finishChromaprint(chromaprintCtx);
                if (fingerprint == null || fingerprint.isEmpty()) {
                    Timber.e("Failed to get fingerprint");
                    notifyRecognitionFailed("Failed to generate fingerprint");
                    return;
                }

                Timber.i("Generated fingerprint, querying AcoustID...");

                // Recognize using API
                RecognitionResult result = apiClient.recognize(fingerprint, FINGERPRINT_DURATION_SEC);
                if (result != null) {
                    Timber.i("Recognized: %s", result);
                    notifyTrackRecognized(result);
                } else {
                    Timber.w("No match found");
                    notifyRecognitionFailed("No match found");
                }

            } finally {
                NativeAudioEngine.freeChromaprint(chromaprintCtx);
            }

        } catch (Exception e) {
            Timber.e(e, "Recognition failed");
            notifyRecognitionFailed(e.getMessage());
        }
    }

    private void notifyTrackRecognized(RecognitionResult result) {
        for (AudioRecognitionListener listener : listeners) {
            listener.onTrackRecognized(result);
        }
    }

    private void notifyRecognitionFailed(String error) {
        for (AudioRecognitionListener listener : listeners) {
            listener.onRecognitionFailed(error);
        }
    }

    private void notifyRecognitionInProgress() {
        for (AudioRecognitionListener listener : listeners) {
            listener.onRecognitionInProgress();
        }
    }
}
