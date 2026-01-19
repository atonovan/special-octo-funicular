package tech.schober.vinylcast.acr;

import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tech.schober.vinylcast.acr.api.AudioRecognitionApiClient;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import tech.schober.vinylcast.audio.AudioStreamProvider;
import tech.schober.vinylcast.audio.NativeAudioEngine;
import tech.schober.vinylcast.utils.VinylCastHelpers;
import timber.log.Timber;

/**
 * Stream provider that performs audio recognition on the audio stream while
 * passing the audio through unchanged (acts as a "tee")
 */
public class AudioRecognitionStreamProvider implements Runnable, AudioStreamProvider {
    private static final int RECOGNITION_INTERVAL_MS = 30000; // Recognize every 30 seconds
    private static final int FINGERPRINT_DURATION_SEC = 15; // Use 15 seconds of audio for fingerprinting
    private static final int BUFFER_SIZE = 4096;

    private final AudioStreamProvider upstreamProvider;
    private final int sampleRate;
    private final int channelCount;
    private final CopyOnWriteArrayList<AudioRecognitionListener> listeners;
    private final AudioRecognitionApiClient apiClient;
    private final ExecutorService executor;

    private InputStream upstreamInputStream;
    private OutputStream downstreamOutputStream;
    private InputStream downstreamInputStream;

    private Thread thread;
    private volatile boolean running = false;
    private long lastRecognitionTime = 0;

    public AudioRecognitionStreamProvider(AudioStreamProvider upstreamProvider,
                                          int sampleRate,
                                          int channelCount,
                                          int bufferSize) throws IOException {
        this.upstreamProvider = upstreamProvider;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.listeners = new CopyOnWriteArrayList<>();
        this.apiClient = new AudioRecognitionApiClient();
        this.executor = Executors.newSingleThreadExecutor();

        // Create piped streams for pass-through
        Pair<OutputStream, InputStream> streams = VinylCastHelpers.getPipedAudioStreams(bufferSize);
        this.downstreamOutputStream = streams.first;
        this.downstreamInputStream = streams.second;
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
        try {
            upstreamInputStream = upstreamProvider.getAudioInputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int samplesNeeded = sampleRate * channelCount * FINGERPRINT_DURATION_SEC;
            short[] fingerprintBuffer = new short[samplesNeeded];
            int fingerprintSampleCount = 0;

            while (running) {
                int bytesRead = upstreamInputStream.read(buffer);
                if (bytesRead <= 0) {
                    Thread.sleep(10);
                    continue;
                }

                // Pass audio through to downstream (this is critical!)
                downstreamOutputStream.write(buffer, 0, bytesRead);
                downstreamOutputStream.flush();

                // Convert bytes to shorts (16-bit PCM) for fingerprinting
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                while (byteBuffer.remaining() >= 2 && fingerprintSampleCount < samplesNeeded) {
                    fingerprintBuffer[fingerprintSampleCount++] = byteBuffer.getShort();
                }

                // Check if we have enough samples and enough time has passed
                long currentTime = System.currentTimeMillis();
                if (fingerprintSampleCount >= samplesNeeded &&
                        (currentTime - lastRecognitionTime) >= RECOGNITION_INTERVAL_MS) {

                    lastRecognitionTime = currentTime;
                    final short[] samplesForRecognition = fingerprintBuffer.clone();

                    Timber.i("Collected %d samples for recognition (%d seconds of audio at %d Hz, %d channels)",
                            samplesForRecognition.length, FINGERPRINT_DURATION_SEC, sampleRate, channelCount);

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
            try {
                if (upstreamInputStream != null) {
                    upstreamInputStream.close();
                }
                if (downstreamOutputStream != null) {
                    downstreamOutputStream.close();
                }
            } catch (IOException e) {
                Timber.e(e, "Failed to close streams");
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

    // AudioStreamProvider interface implementation
    @Override
    public InputStream getAudioInputStream() {
        return downstreamInputStream;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public int getChannelCount() {
        return channelCount;
    }

    @Override
    public int getAudioEncoding() {
        return upstreamProvider.getAudioEncoding();
    }
}
