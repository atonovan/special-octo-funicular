package tech.schober.vinylcast.acr.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import tech.schober.vinylcast.acr.model.ITunesResponse;

/**
 * Retrofit API interface for iTunes Search API
 * Free API with no authentication required
 * Provides album artwork at up to 5000x5000px resolution
 */
public interface ITunesApi {
    /**
     * Search for album artwork
     * @param term Search term (artist + album name)
     * @param entity Entity type (album)
     * @param limit Number of results to return
     * @return iTunes search response with artwork URLs
     */
    @GET("search")
    Call<ITunesResponse> searchAlbum(
            @Query("term") String term,
            @Query("entity") String entity,
            @Query("limit") int limit
    );
}
