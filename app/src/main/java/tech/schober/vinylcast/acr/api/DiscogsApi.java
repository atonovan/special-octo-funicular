package tech.schober.vinylcast.acr.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import tech.schober.vinylcast.acr.model.DiscogsResponse;

/**
 * Retrofit API interface for Discogs API
 * Free API for vinyl/music database lookups
 * https://www.discogs.com/developers
 */
public interface DiscogsApi {
    /**
     * Search Discogs database by barcode
     * @param barcode UPC/EAN barcode from album
     * @param type Search type (release for albums)
     * @param token User token for authentication
     * @return Discogs search response with release information
     */
    @GET("database/search")
    Call<DiscogsResponse> searchByBarcode(
            @Query("barcode") String barcode,
            @Query("type") String type,
            @Query("token") String token
    );
}
