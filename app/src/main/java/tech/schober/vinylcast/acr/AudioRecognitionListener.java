package tech.schober.vinylcast.acr;

import tech.schober.vinylcast.acr.model.RecognitionResult;

/**
 * Listener interface for audio recognition events
 */
public interface AudioRecognitionListener {
    /**
     * Called when a track is successfully recognized
     * @param result The recognition result containing track metadata
     */
    void onTrackRecognized(RecognitionResult result);

    /**
     * Called when recognition fails
     * @param error Error message describing the failure
     */
    void onRecognitionFailed(String error);

    /**
     * Called when recognition is in progress
     */
    void onRecognitionInProgress();
}
