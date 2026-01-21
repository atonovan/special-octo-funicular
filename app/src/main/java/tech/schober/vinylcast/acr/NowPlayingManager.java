package tech.schober.vinylcast.acr;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import tech.schober.vinylcast.acr.model.DiscogsReleaseDetails;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import timber.log.Timber;

/**
 * Manages the currently playing album information
 * Persists across app restarts
 */
public class NowPlayingManager {
    private static final String PREFS_NAME = "now_playing";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ALBUM = "album";
    private static final String KEY_TITLE = "title";
    private static final String KEY_YEAR = "year";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_HAS_ARTWORK = "has_artwork";
    private static final String KEY_TRACKLIST_JSON = "tracklist_json";
    private static final String ARTWORK_FILENAME = "now_playing_artwork.jpg";

    private static NowPlayingManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final List<NowPlayingListener> listeners = new ArrayList<>();
    private final Gson gson = new Gson();

    private RecognitionResult currentlyPlaying;

    public interface NowPlayingListener {
        void onNowPlayingChanged(RecognitionResult result);
    }

    private NowPlayingManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized NowPlayingManager getInstance(Context context) {
        if (instance == null) {
            instance = new NowPlayingManager(context);
        }
        return instance;
    }

    public void addListener(NowPlayingListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NowPlayingListener listener) {
        listeners.remove(listener);
    }

    public void setNowPlaying(RecognitionResult result) {
        this.currentlyPlaying = result;
        saveToPrefs();
        notifyListeners();
    }

    public RecognitionResult getNowPlaying() {
        return currentlyPlaying;
    }

    public void clear() {
        this.currentlyPlaying = null;
        clearPrefs();
        notifyListeners();
    }

    private void saveToPrefs() {
        SharedPreferences.Editor editor = prefs.edit();

        if (currentlyPlaying == null) {
            editor.clear();
        } else {
            editor.putString(KEY_ARTIST, currentlyPlaying.getArtist());
            editor.putString(KEY_ALBUM, currentlyPlaying.getAlbum());
            editor.putString(KEY_TITLE, currentlyPlaying.getTitle());

            // Save year
            if (currentlyPlaying.getYear() != null) {
                editor.putInt(KEY_YEAR, currentlyPlaying.getYear());
            } else {
                editor.remove(KEY_YEAR);
            }

            // Save start time
            editor.putLong(KEY_START_TIME, currentlyPlaying.getStartTimeMillis());

            // Save tracklist as JSON
            if (currentlyPlaying.getTracklist() != null && !currentlyPlaying.getTracklist().isEmpty()) {
                String tracklistJson = gson.toJson(currentlyPlaying.getTracklist());
                editor.putString(KEY_TRACKLIST_JSON, tracklistJson);
            } else {
                editor.remove(KEY_TRACKLIST_JSON);
            }

            // Save artwork to file
            if (currentlyPlaying.getAlbumArtwork() != null) {
                saveArtworkToFile(currentlyPlaying.getAlbumArtwork());
                editor.putBoolean(KEY_HAS_ARTWORK, true);
            } else {
                editor.putBoolean(KEY_HAS_ARTWORK, false);
            }
        }

        editor.apply();
    }

    private void loadFromPrefs() {
        String artist = prefs.getString(KEY_ARTIST, null);
        String album = prefs.getString(KEY_ALBUM, null);
        String title = prefs.getString(KEY_TITLE, null);
        boolean hasArtwork = prefs.getBoolean(KEY_HAS_ARTWORK, false);

        if (artist != null && album != null) {
            currentlyPlaying = new RecognitionResult(artist, album, title, null);

            // Load year
            if (prefs.contains(KEY_YEAR)) {
                currentlyPlaying.setYear(prefs.getInt(KEY_YEAR, 0));
            }

            // Load start time
            long startTime = prefs.getLong(KEY_START_TIME, 0);
            currentlyPlaying.setStartTimeMillis(startTime);

            // Load tracklist from JSON
            String tracklistJson = prefs.getString(KEY_TRACKLIST_JSON, null);
            if (tracklistJson != null) {
                try {
                    Type tracklistType = new TypeToken<List<DiscogsReleaseDetails.Track>>(){}.getType();
                    List<DiscogsReleaseDetails.Track> tracklist = gson.fromJson(tracklistJson, tracklistType);
                    currentlyPlaying.setTracklist(tracklist);
                } catch (Exception e) {
                    Timber.w(e, "Failed to parse tracklist JSON");
                }
            }

            if (hasArtwork) {
                Bitmap artwork = loadArtworkFromFile();
                if (artwork != null) {
                    currentlyPlaying.setAlbumArtwork(artwork);
                }
            }
        }
    }

    private void clearPrefs() {
        prefs.edit().clear().apply();
        deleteArtworkFile();
    }

    private void saveArtworkToFile(Bitmap bitmap) {
        File artworkFile = new File(context.getFilesDir(), ARTWORK_FILENAME);
        try (FileOutputStream out = new FileOutputStream(artworkFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            Timber.d("Saved artwork to file");
        } catch (IOException e) {
            Timber.e(e, "Failed to save artwork to file");
        }
    }

    private Bitmap loadArtworkFromFile() {
        File artworkFile = new File(context.getFilesDir(), ARTWORK_FILENAME);
        if (artworkFile.exists()) {
            return BitmapFactory.decodeFile(artworkFile.getAbsolutePath());
        }
        return null;
    }

    private void deleteArtworkFile() {
        File artworkFile = new File(context.getFilesDir(), ARTWORK_FILENAME);
        if (artworkFile.exists()) {
            artworkFile.delete();
        }
    }

    private void notifyListeners() {
        for (NowPlayingListener listener : listeners) {
            listener.onNowPlayingChanged(currentlyPlaying);
        }
    }
}
