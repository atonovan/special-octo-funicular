package tech.schober.vinylcast.acr.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import tech.schober.vinylcast.acr.model.AcoustIdResponse;

/**
 * Retrofit API interface for AcoustID service
 */
public interface AcoustIdApi {
    /**
     * Submit fingerprint to AcoustID for recognition
     * @param client API client key
     * @param fingerprint Chromaprint fingerprint
     * @param duration Duration of the audio sample in seconds
     * @param meta Additional metadata to include in response (recordings, releases, artists)
     * @return AcoustID response with matching recordings
     */
    @GET("v2/lookup")
    Call<AcoustIdResponse> lookup(
            @Query("client") String client,
            @Query("fingerprint") String fingerprint,
            @Query("duration") int duration,
            @Query("meta") String meta
    );
}
