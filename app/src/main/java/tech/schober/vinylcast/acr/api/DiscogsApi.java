package tech.schober.vinylcast.acr.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tech.schober.vinylcast.acr.model.DiscogsCollectionResponse;
import tech.schober.vinylcast.acr.model.DiscogsIdentityResponse;
import tech.schober.vinylcast.acr.model.DiscogsReleaseDetails;
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

    /**
     * Get detailed release information including tracklist and personnel
     * @param releaseId Discogs release ID
     * @param token User token for authentication
     * @return Detailed release information
     */
    @GET("releases/{release_id}")
    Call<DiscogsReleaseDetails> getReleaseDetails(
            @Path("release_id") long releaseId,
            @Query("token") String token
    );

    /**
     * Get current user's identity
     * @param token User token for authentication
     * @return User identity with username
     */
    @GET("oauth/identity")
    Call<DiscogsIdentityResponse> getIdentity(
            @Query("token") String token
    );

    /**
     * Get user's collection folder items
     * @param username Discogs username
     * @param folderId Folder ID (0 = All, 1 = Uncategorized)
     * @param token User token for authentication
     * @param page Page number
     * @param perPage Items per page
     * @return Collection response with releases
     */
    @GET("users/{username}/collection/folders/{folder_id}/releases")
    Call<DiscogsCollectionResponse> getCollectionItems(
            @Path("username") String username,
            @Path("folder_id") int folderId,
            @Query("token") String token,
            @Query("page") int page,
            @Query("per_page") int perPage
    );

    /**
     * Add release to user's collection
     * @param username Discogs username
     * @param folderId Folder ID (1 = Uncategorized/default collection)
     * @param releaseId Release ID to add
     * @param token User token for authentication
     * @param body Empty body (required by Discogs API)
     * @return void
     */
    @POST("users/{username}/collection/folders/{folder_id}/releases/{release_id}")
    Call<Void> addToCollection(
            @Path("username") String username,
            @Path("folder_id") int folderId,
            @Path("release_id") long releaseId,
            @Query("token") String token,
            @Body Object body
    );
}
