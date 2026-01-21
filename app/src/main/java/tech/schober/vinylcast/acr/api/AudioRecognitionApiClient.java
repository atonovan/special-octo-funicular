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
import tech.schober.vinylcast.BuildConfig;
import tech.schober.vinylcast.acr.model.AcoustIdResponse;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import timber.log.Timber;

/**
 * Client for audio recognition APIs
 */
public class AudioRecognitionApiClient {
    // API key loaded from local.properties via BuildConfig
    // Get your own free API key from https://acoustid.org/new-application
    private static final String ACOUSTID_API_KEY = BuildConfig.ACOUSTID_API_KEY;
    private static final String ACOUSTID_BASE_URL = "https://api.acoustid.org/";
    private static final String ITUNES_BASE_URL = "https://itunes.apple.com/";

    private final AcoustIdApi acoustIdApi;
    private final ITunesApi iTunesApi;

    public AudioRecognitionApiClient() {
        // Log API key status (first few chars only for security)
        if (ACOUSTID_API_KEY == null || ACOUSTID_API_KEY.equals("YOUR_API_KEY_HERE") || ACOUSTID_API_KEY.isEmpty()) {
            Timber.e("AcoustID API key is not configured! Recognition will fail.");
        } else {
            String keyPreview = ACOUSTID_API_KEY.length() > 4 ?
                ACOUSTID_API_KEY.substring(0, 4) + "..." : "***";
            Timber.i("AcoustID API key loaded: %s", keyPreview);
        }

        OkHttpClient client = new OkHttpClient.Builder().build();

        Retrofit acoustIdRetrofit = new Retrofit.Builder()
                .baseUrl(ACOUSTID_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Retrofit iTunesRetrofit = new Retrofit.Builder()
                .baseUrl(ITUNES_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        acoustIdApi = acoustIdRetrofit.create(AcoustIdApi.class);
        iTunesApi = iTunesRetrofit.create(ITunesApi.class);
    }

    /**
     * Recognize audio from fingerprint
     * @param fingerprint Chromaprint fingerprint
     * @param duration Duration in seconds
     * @return Recognition result or null if failed
     */
    public RecognitionResult recognize(String fingerprint, int duration) {
        try {
            Timber.i("Making AcoustID API POST request: duration=%ds, fingerprint length=%d",
                    duration, fingerprint.length());

            // Log full fingerprint for manual testing
            Timber.d("Full fingerprint: %s", fingerprint);
            Timber.d("Manual test URL (GET): https://api.acoustid.org/v2/lookup?client=%s&duration=%d&meta=recordings+usermeta&fingerprint=%s",
                    ACOUSTID_API_KEY, duration, fingerprint.substring(0, Math.min(100, fingerprint.length())) + "...");

            // Use POST (recommended by AcoustID docs for long fingerprints)
            // Request usermeta (user-submitted artist/title/album) - gives access to all 70M fingerprints
            // instead of just the 20M with MusicBrainz recordings
            Call<AcoustIdResponse> call = acoustIdApi.lookup(
                    ACOUSTID_API_KEY,
                    fingerprint,
                    duration,
                    "recordings usermeta"
            );

            Response<AcoustIdResponse> response = call.execute();
            if (!response.isSuccessful() || response.body() == null) {
                String errorBody = "";
                if (response.errorBody() != null) {
                    try {
                        errorBody = response.errorBody().string();
                    } catch (Exception e) {
                        errorBody = "[unable to read error body]";
                    }
                }
                Timber.e("AcoustID API request failed: HTTP %d %s - %s",
                        response.code(), response.message(), errorBody);
                return null;
            }

            AcoustIdResponse acoustIdResponse = response.body();

            // Log the full API response for debugging
            Timber.d("AcoustID API response: status=%s, results count=%d",
                    acoustIdResponse.getStatus(),
                    acoustIdResponse.getResults() != null ? acoustIdResponse.getResults().size() : 0);

            if (acoustIdResponse.getResults() == null || acoustIdResponse.getResults().isEmpty()) {
                Timber.w("No recognition results found");
                return null;
            }

            // Get the first result with the highest score
            AcoustIdResponse.Result result = acoustIdResponse.getResults().get(0);
            Timber.d("Top result: score=%.2f, id=%s, sources=%d",
                    result.getScore(),
                    result.getId(),
                    result.getSources());

            String title = null;
            String artist = null;
            String album = null;

            // Try to get metadata from recordings first (MusicBrainz data)
            if (result.getRecordings() != null && !result.getRecordings().isEmpty()) {
                AcoustIdResponse.Recording recording = result.getRecordings().get(0);
                title = recording.getTitle();

                if (recording.getArtists() != null && !recording.getArtists().isEmpty()) {
                    artist = recording.getArtists().get(0).getName();
                }

                if (recording.getReleases() != null && !recording.getReleases().isEmpty()) {
                    album = recording.getReleases().get(0).getTitle();
                }

                Timber.d("Got metadata from MusicBrainz: artist=%s, album=%s, title=%s", artist, album, title);
            }

            // If we didn't get metadata from MusicBrainz, try usermeta (user-submitted data)
            if ((title == null || artist == null) && result.getRecordings() != null && !result.getRecordings().isEmpty()) {
                AcoustIdResponse.Recording recording = result.getRecordings().get(0);
                if (recording.getUsermetadata() != null && !recording.getUsermetadata().isEmpty()) {
                    AcoustIdResponse.UserMeta userMeta = recording.getUsermetadata().get(0);
                    if (title == null) title = userMeta.getTitle();
                    if (artist == null) artist = userMeta.getArtist();
                    if (album == null) album = userMeta.getAlbum();

                    Timber.d("Got metadata from usermeta: artist=%s, album=%s, title=%s", artist, album, title);
                }
            }

            // If we still don't have basic metadata, give up
            if (title == null || artist == null) {
                Timber.w("No usable metadata found (need at least title and artist)");
                return null;
            }

            RecognitionResult recognitionResult = new RecognitionResult(artist, album, title, null);

            // Fetch album artwork from iTunes
            if (artist != null && album != null) {
                Bitmap artwork = fetchArtworkFromItunes(artist, album);
                recognitionResult.setAlbumArtwork(artwork);
            }

            return recognitionResult;

        } catch (IOException e) {
            Timber.e(e, "Failed to recognize audio");
            return null;
        }
    }

    /**
     * Fetch album artwork from iTunes Search API
     * @param artist Artist name
     * @param album Album name
     * @return Bitmap of album artwork or null if failed
     */
    private Bitmap fetchArtworkFromItunes(String artist, String album) {
        try {
            // Build search term from artist and album
            String searchTerm = artist + " " + album;
            Timber.d("Searching iTunes for artwork: %s", searchTerm);

            Call<tech.schober.vinylcast.acr.model.ITunesResponse> call =
                iTunesApi.searchAlbum(searchTerm, "album", 1);
            Response<tech.schober.vinylcast.acr.model.ITunesResponse> response = call.execute();

            if (!response.isSuccessful() || response.body() == null) {
                Timber.w("iTunes search failed: HTTP %d", response.code());
                return null;
            }

            tech.schober.vinylcast.acr.model.ITunesResponse iTunesResponse = response.body();
            if (iTunesResponse.getResultCount() == 0 || iTunesResponse.getResults().isEmpty()) {
                Timber.w("No iTunes results for: %s", searchTerm);
                return null;
            }

            // Get high-resolution artwork URL (1200x1200)
            tech.schober.vinylcast.acr.model.ITunesResponse.Result result = iTunesResponse.getResults().get(0);
            String artworkUrl = result.getArtworkUrl(1200);

            if (artworkUrl == null) {
                Timber.w("No artwork URL found in iTunes result");
                return null;
            }

            Timber.d("Downloading artwork from: %s", artworkUrl);

            // Download the artwork
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(artworkUrl)
                    .build();

            okhttp3.Response artworkResponse = new OkHttpClient().newCall(request).execute();
            if (!artworkResponse.isSuccessful() || artworkResponse.body() == null) {
                Timber.w("Failed to download artwork: HTTP %d", artworkResponse.code());
                return null;
            }

            InputStream inputStream = artworkResponse.body().byteStream();
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                Timber.i("Successfully fetched artwork: %dx%d", bitmap.getWidth(), bitmap.getHeight());
            }

            return bitmap;

        } catch (IOException e) {
            Timber.e(e, "Error fetching iTunes artwork");
            return null;
        }
    }
}
