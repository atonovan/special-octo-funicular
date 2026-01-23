package tech.schober.vinylcast.acr;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.preference.PreferenceManager;

import tech.schober.vinylcast.R;
import tech.schober.vinylcast.acr.model.DiscogsReleaseDetails;
import tech.schober.vinylcast.acr.model.RecognitionResult;
import timber.log.Timber;

/**
 * Fragment that displays the currently playing album with artwork
 * and a color-extracted blurred background
 */
public class NowPlayingFragment extends Fragment implements NowPlayingManager.NowPlayingListener {
    private View blurredBackground;
    private ImageView albumArtwork;
    private TextView trackTitle;
    private TextView trackArtist;
    private TextView trackYear;
    private TextView timeCounter;
    private TextView currentTrack;
    private android.widget.Button flipRecordButton;

    private NowPlayingManager nowPlayingManager;
    private SharedPreferences prefs;
    private Handler trackUpdateHandler = new Handler(Looper.getMainLooper());
    private boolean isFullscreen = false;

    // Update current track every 5 seconds
    private static final int TRACK_UPDATE_INTERVAL_MS = 5000;

    private final Runnable trackUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateCurrentTrack();
            trackUpdateHandler.postDelayed(this, TRACK_UPDATE_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        blurredBackground = view.findViewById(R.id.blurred_background);
        albumArtwork = view.findViewById(R.id.album_artwork);
        trackTitle = view.findViewById(R.id.track_title);
        trackArtist = view.findViewById(R.id.track_artist);
        trackYear = view.findViewById(R.id.track_year);
        timeCounter = view.findViewById(R.id.time_counter);
        currentTrack = view.findViewById(R.id.current_track);
        flipRecordButton = view.findViewById(R.id.flip_record_button);

        nowPlayingManager = NowPlayingManager.getInstance(requireContext());
        nowPlayingManager.addListener(this);

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Add tap gesture for fullscreen toggle
        albumArtwork.setOnClickListener(v -> toggleFullscreen());

        // Set up flip record button
        flipRecordButton.setOnClickListener(v -> {
            nowPlayingManager.flipRecordToSideB();
            // Update UI immediately
            updateCurrentTrack();
        });

        // Load current Now Playing info
        updateNowPlaying(nowPlayingManager.getNowPlaying());

        // Start track progress updates
        trackUpdateHandler.post(trackUpdateRunnable);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        trackUpdateHandler.removeCallbacks(trackUpdateRunnable);
        if (nowPlayingManager != null) {
            nowPlayingManager.removeListener(this);
        }
    }

    @Override
    public void onNowPlayingChanged(RecognitionResult result) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> updateNowPlaying(result));
        }
    }

    private void updateNowPlaying(RecognitionResult result) {
        if (result == null) {
            // Show default/empty state
            albumArtwork.setImageResource(R.drawable.vinyl_orange_512);
            trackTitle.setText("No album selected");
            trackArtist.setText("Scan a barcode to get started");
            trackYear.setVisibility(View.GONE);
            currentTrack.setVisibility(View.GONE);
            setBackgroundColor(0xFF1A1A1A); // Dark gray
            return;
        }

        // Update text
        trackTitle.setText(result.getAlbum() != null ? result.getAlbum() : "Unknown Album");
        trackArtist.setText(result.getArtist() != null ? result.getArtist() : "Unknown Artist");

        // Update year
        if (result.getYear() != null && result.getYear() > 0) {
            trackYear.setText(String.valueOf(result.getYear()));
            trackYear.setVisibility(View.VISIBLE);
        } else {
            trackYear.setVisibility(View.GONE);
        }

        // Update current track
        updateCurrentTrack();

        // Update artwork and extract colors
        if (result.getAlbumArtwork() != null) {
            albumArtwork.setImageBitmap(result.getAlbumArtwork());
            extractColorsAndSetBackground(result.getAlbumArtwork());
        } else {
            albumArtwork.setImageResource(R.drawable.vinyl_orange_512);
            setBackgroundColor(0xFF1A1A1A);
        }
    }

    private void updateCurrentTrack() {
        RecognitionResult result = nowPlayingManager.getNowPlaying();
        if (result == null) {
            currentTrack.setVisibility(View.GONE);
            timeCounter.setVisibility(View.GONE);
            flipRecordButton.setVisibility(View.GONE);
            return;
        }

        boolean trackProgressEnabled = isTrackProgressEnabled();
        boolean showTimeCounter = prefs.getBoolean("prefs_key_show_time_counter", true);

        // Update time counter
        if (showTimeCounter && result.getTracklist() != null && !result.getTracklist().isEmpty()) {
            long elapsedSeconds = (System.currentTimeMillis() - result.getStartTimeMillis()) / 1000;
            int totalSeconds = getTotalDurationSeconds(result);

            String elapsed = formatTime(elapsedSeconds);
            String total = formatTime(totalSeconds);
            timeCounter.setText(elapsed + " / " + total);
            timeCounter.setVisibility(View.VISIBLE);
        } else {
            timeCounter.setVisibility(View.GONE);
        }

        DiscogsReleaseDetails.Track track = result.getCurrentTrack(trackProgressEnabled);

        if (track != null && trackProgressEnabled) {
            String trackInfo = "Now Playing: " + track.getPosition() + ". " + track.getTitle();
            currentTrack.setText(trackInfo);
            currentTrack.setVisibility(View.VISIBLE);

            // Show flip button if on side A
            if (result.shouldShowFlipButton()) {
                flipRecordButton.setVisibility(View.VISIBLE);
            } else {
                flipRecordButton.setVisibility(View.GONE);
            }
        } else {
            currentTrack.setVisibility(View.GONE);
            flipRecordButton.setVisibility(View.GONE);
        }
    }

    private int getTotalDurationSeconds(RecognitionResult result) {
        int total = 0;
        if (result.getTracklist() != null) {
            for (DiscogsReleaseDetails.Track track : result.getTracklist()) {
                if (track.getType() != null && track.getType().equals("track")) {
                    total += track.getDurationSeconds();
                }
            }
        }
        return total;
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private boolean isTrackProgressEnabled() {
        return prefs.getBoolean("prefs_key_track_progress_enabled", true);
    }

    private void extractColorsAndSetBackground(Bitmap bitmap) {
        // Generate palette from artwork
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) {
                setBackgroundColor(0xFF1A1A1A);
                return;
            }

            // Get vibrant or dominant color
            int defaultColor = 0xFF1A1A1A;
            int vibrantColor = palette.getVibrantColor(defaultColor);
            int darkVibrantColor = palette.getDarkVibrantColor(defaultColor);
            int dominantColor = palette.getDominantColor(defaultColor);

            // Use dark vibrant as primary, fall back to vibrant, then dominant
            int primaryColor = darkVibrantColor != defaultColor ? darkVibrantColor :
                    vibrantColor != defaultColor ? vibrantColor : dominantColor;

            // Darken the color a bit for background
            int darkerColor = darkenColor(primaryColor, 0.5f);

            Timber.d("Extracted colors - Primary: #%06X, Darker: #%06X",
                    primaryColor & 0xFFFFFF, darkerColor & 0xFFFFFF);

            setGradientBackground(darkerColor, primaryColor);

            // Apply blur effect on Android 12+ for frosted glass look
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applyBlurEffect(bitmap);
            }
        });
    }

    private void applyBlurEffect(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // Create blurred version of the artwork for the background
                RenderEffect blurEffect = RenderEffect.createBlurEffect(
                        50f, 50f, Shader.TileMode.CLAMP);
                blurredBackground.setRenderEffect(blurEffect);

                // Set the artwork as background (will be blurred by RenderEffect)
                blurredBackground.setBackground(new BitmapDrawable(getResources(), bitmap));

                Timber.d("Applied RenderEffect blur for frosted glass effect");
            } catch (Exception e) {
                Timber.w(e, "Failed to apply blur effect, using gradient fallback");
            }
        }
    }

    private int darkenColor(int color, float factor) {
        int a = android.graphics.Color.alpha(color);
        int r = Math.round(android.graphics.Color.red(color) * factor);
        int g = Math.round(android.graphics.Color.green(color) * factor);
        int b = Math.round(android.graphics.Color.blue(color) * factor);
        return android.graphics.Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

    private void setBackgroundColor(int color) {
        blurredBackground.setBackground(null); // Clear any render effects
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurredBackground.setRenderEffect(null);
        }
        blurredBackground.setBackgroundColor(color);
    }

    private void setGradientBackground(int startColor, int endColor) {
        // Clear render effect if using gradient
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurredBackground.setRenderEffect(null);
        }

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
        );
        gradient.setCornerRadius(0f);
        blurredBackground.setBackground(gradient);
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;

        if (getActivity() == null) {
            return;
        }

        androidx.appcompat.app.ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) getActivity()).getSupportActionBar();

        if (isFullscreen) {
            // Hide action bar
            if (actionBar != null) {
                actionBar.hide();
            }

            // Hide system UI (status bar, navigation bar) but keep metadata visible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getActivity().getWindow().setDecorFitsSystemWindows(false);
                WindowInsetsController controller = getActivity().getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                // For older Android versions
                getActivity().getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            Timber.d("Entered fullscreen mode - action bar and system UI hidden");
        } else {
            // Show action bar
            if (actionBar != null) {
                actionBar.show();
            }

            // Show system UI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getActivity().getWindow().setDecorFitsSystemWindows(true);
                WindowInsetsController controller = getActivity().getWindow().getInsetsController();
                if (controller != null) {
                    controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                }
            } else {
                getActivity().getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_VISIBLE);
            }

            Timber.d("Exited fullscreen mode");
        }
    }
}
