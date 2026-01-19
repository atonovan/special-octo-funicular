package tech.schober.vinylcast.acr.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit API interface for Cover Art Archive
 */
public interface CoverArtApi {
    /**
     * Get front cover art for a release
     * @param mbid MusicBrainz Release ID
     * @return Image data as ResponseBody
     */
    @GET("release/{mbid}/front-500")
    Call<ResponseBody> getFrontCover(@Path("mbid") String mbid);
}
