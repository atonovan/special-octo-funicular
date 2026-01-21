package tech.schober.vinylcast.acr.api;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import tech.schober.vinylcast.acr.model.AcoustIdResponse;

/**
 * Retrofit API interface for AcoustID service
 */
public interface AcoustIdApi {
    /**
     * Submit fingerprint to AcoustID for recognition
     * Uses POST instead of GET because fingerprints can be very long (3000+ chars)
     * @param client API client key
     * @param fingerprint Chromaprint fingerprint
     * @param duration Duration of the audio sample in seconds
     * @param meta Additional metadata to include in response (recordings, releases, artists)
     * @return AcoustID response with matching recordings
     */
    @FormUrlEncoded
    @POST("v2/lookup")
    Call<AcoustIdResponse> lookup(
            @Field("client") String client,
            @Field("fingerprint") String fingerprint,
            @Field("duration") int duration,
            @Field("meta") String meta
    );
}
