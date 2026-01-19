package tech.schober.vinylcast.acr.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import tech.schober.vinylcast.acr.model.AcoustIdResponse;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import timber.log.Timber;

/**
 * Client for audio recognition APIs
 */
public class AudioRecognitionApiClient {
    // TODO: Get your own API key from https://acoustid.org/new-application
    private static final String ACOUSTID_API_KEY = "YOUR_API_KEY_HERE";
    private static final String ACOUSTID_BASE_URL = "https://api.acoustid.org/";
    private static final String COVERART_BASE_URL = "https://coverartarchive.org/";

    private final AcoustIdApi acoustIdApi;
    private final CoverArtApi coverArtApi;

    public AudioRecognitionApiClient() {
        OkHttpClient client = new OkHttpClient.Builder().build();

        Retrofit acoustIdRetrofit = new Retrofit.Builder()
                .baseUrl(ACOUSTID_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Retrofit coverArtRetrofit = new Retrofit.Builder()
                .baseUrl(COVERART_BASE_URL)
                .client(client)
                .build();

        acoustIdApi = acoustIdRetrofit.create(AcoustIdApi.class);
        coverArtApi = coverArtRetrofit.create(CoverArtApi.class);
    }

    /**
     * Recognize audio from fingerprint
     * @param fingerprint Chromaprint fingerprint
     * @param duration Duration in seconds
     * @return Recognition result or null if failed
     */
    public RecognitionResult recognize(String fingerprint, int duration) {
        try {
            Call<AcoustIdResponse> call = acoustIdApi.lookup(
                    ACOUSTID_API_KEY,
                    fingerprint,
                    duration,
                    "recordings releases artists"
            );

            Response<AcoustIdResponse> response = call.execute();
            if (!response.isSuccessful() || response.body() == null) {
                Timber.e("AcoustID API request failed: %s", response.message());
                return null;
            }

            AcoustIdResponse acoustIdResponse = response.body();
            if (acoustIdResponse.getResults() == null || acoustIdResponse.getResults().isEmpty()) {
                Timber.w("No recognition results found");
                return null;
            }

            // Get the first result with the highest score
            AcoustIdResponse.Result result = acoustIdResponse.getResults().get(0);
            if (result.getRecordings() == null || result.getRecordings().isEmpty()) {
                Timber.w("No recordings found in result");
                return null;
            }

            AcoustIdResponse.Recording recording = result.getRecordings().get(0);
            String title = recording.getTitle();
            String artist = "";
            String album = "";
            String releaseId = "";

            if (recording.getArtists() != null && !recording.getArtists().isEmpty()) {
                artist = recording.getArtists().get(0).getName();
            }

            if (recording.getReleases() != null && !recording.getReleases().isEmpty()) {
                AcoustIdResponse.Release release = recording.getReleases().get(0);
                album = release.getTitle();
                releaseId = release.getId();
            }

            RecognitionResult recognitionResult = new RecognitionResult(artist, album, title, releaseId);

            // Fetch album artwork if we have a release ID
            if (releaseId != null && !releaseId.isEmpty()) {
                Bitmap artwork = fetchAlbumArtwork(releaseId);
                recognitionResult.setAlbumArtwork(artwork);
            }

            return recognitionResult;

        } catch (IOException e) {
            Timber.e(e, "Failed to recognize audio");
            return null;
        }
    }

    /**
     * Fetch album artwork from Cover Art Archive
     * @param releaseId MusicBrainz release ID
     * @return Bitmap of album artwork or null if failed
     */
    private Bitmap fetchAlbumArtwork(String releaseId) {
        try {
            Call<ResponseBody> call = coverArtApi.getFrontCover(releaseId);
            Response<ResponseBody> response = call.execute();

            if (!response.isSuccessful() || response.body() == null) {
                Timber.w("Failed to fetch album artwork for release: %s", releaseId);
                return null;
            }

            InputStream inputStream = response.body().byteStream();
            return BitmapFactory.decodeStream(inputStream);

        } catch (IOException e) {
            Timber.e(e, "Error fetching album artwork");
            return null;
        }
    }
}
