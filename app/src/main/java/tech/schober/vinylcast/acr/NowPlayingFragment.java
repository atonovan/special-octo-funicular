package tech.schober.vinylcast.acr;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import tech.schober.vinylcast.R;
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

    private NowPlayingManager nowPlayingManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_now_playing, container, false);

        blurredBackground = view.findViewById(R.id.blurred_background);
        albumArtwork = view.findViewById(R.id.album_artwork);
        trackTitle = view.findViewById(R.id.track_title);
        trackArtist = view.findViewById(R.id.track_artist);

        nowPlayingManager = NowPlayingManager.getInstance(requireContext());
        nowPlayingManager.addListener(this);

        // Load current Now Playing info
        updateNowPlaying(nowPlayingManager.getNowPlaying());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
            setBackgroundColor(0xFF1A1A1A); // Dark gray
            return;
        }

        // Update text
        trackTitle.setText(result.getAlbum() != null ? result.getAlbum() : "Unknown Album");
        trackArtist.setText(result.getArtist() != null ? result.getArtist() : "Unknown Artist");

        // Update artwork and extract colors
        if (result.getAlbumArtwork() != null) {
            albumArtwork.setImageBitmap(result.getAlbumArtwork());
            extractColorsAndSetBackground(result.getAlbumArtwork());
        } else {
            albumArtwork.setImageResource(R.drawable.vinyl_orange_512);
            setBackgroundColor(0xFF1A1A1A);
        }
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
        });
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
        blurredBackground.setBackgroundColor(color);
    }

    private void setGradientBackground(int startColor, int endColor) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
        );
        gradient.setCornerRadius(0f);
        blurredBackground.setBackground(gradient);
    }
}
