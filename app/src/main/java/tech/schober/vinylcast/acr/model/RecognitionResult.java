package tech.schober.vinylcast.acr.model;

import android.graphics.Bitmap;
import java.util.List;

/**
 * Represents the result of audio recognition
 */
public class RecognitionResult {
    private final String artist;
    private final String album;
    private final String title;
    private final String releaseId;  // MusicBrainz release ID (String)
    private final Long discogsReleaseId;  // Discogs release ID (Long)
    private Bitmap albumArtwork;

    // Extended metadata
    private Integer year;
    private List<DiscogsReleaseDetails.Track> tracklist;
    private long startTimeMillis;  // When this album was set as Now Playing

    public RecognitionResult(String artist, String album, String title, String releaseId) {
        this(artist, album, title, releaseId, null);
    }

    public RecognitionResult(String artist, String album, String title, String releaseId, Long discogsReleaseId) {
        this.artist = artist;
        this.album = album;
        this.title = title;
        this.releaseId = releaseId;
        this.discogsReleaseId = discogsReleaseId;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getTitle() {
        return title;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public Long getDiscogsReleaseId() {
        return discogsReleaseId;
    }

    public Bitmap getAlbumArtwork() {
        return albumArtwork;
    }

    public void setAlbumArtwork(Bitmap albumArtwork) {
        this.albumArtwork = albumArtwork;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<DiscogsReleaseDetails.Track> getTracklist() {
        return tracklist;
    }

    public void setTracklist(List<DiscogsReleaseDetails.Track> tracklist) {
        this.tracklist = tracklist;
    }

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    /**
     * Get currently playing track based on elapsed time
     * @param enableTrackProgress Whether track progress estimation is enabled
     * @return Current track or null if disabled/not available
     */
    public DiscogsReleaseDetails.Track getCurrentTrack(boolean enableTrackProgress) {
        if (!enableTrackProgress || tracklist == null || tracklist.isEmpty() || startTimeMillis == 0) {
            return null;
        }

        long elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
        int cumulativeSeconds = 0;

        for (DiscogsReleaseDetails.Track track : tracklist) {
            // Skip non-track items (headings, etc.)
            if (track.getType() != null && !track.getType().equals("track")) {
                continue;
            }

            int trackDuration = track.getDurationSeconds();
            if (trackDuration == 0) {
                continue; // Skip tracks without duration
            }

            cumulativeSeconds += trackDuration;
            if (elapsedSeconds < cumulativeSeconds) {
                return track;
            }
        }

        // If we've played past the end, return the last track
        for (int i = tracklist.size() - 1; i >= 0; i--) {
            if (tracklist.get(i).getType() != null &&
                tracklist.get(i).getType().equals("track")) {
                return tracklist.get(i);
            }
        }

        return null;
    }

    /**
     * Check if current track is on side A
     * @return true if on side A, false if on side B or unknown
     */
    public boolean isOnSideA() {
        DiscogsReleaseDetails.Track currentTrack = getCurrentTrack(true);
        if (currentTrack == null || currentTrack.getPosition() == null) {
            return true; // Default to side A
        }
        String position = currentTrack.getPosition().toUpperCase();
        return position.startsWith("A") || position.startsWith("1");
    }

    /**
     * Get the total duration of side A in seconds
     * @return Duration of side A in seconds, or 0 if not available
     */
    public int getSideADurationSeconds() {
        if (tracklist == null || tracklist.isEmpty()) {
            return 0;
        }

        int duration = 0;
        for (DiscogsReleaseDetails.Track track : tracklist) {
            if (track.getType() != null && !track.getType().equals("track")) {
                continue;
            }
            if (track.getPosition() == null) {
                continue;
            }

            String position = track.getPosition().toUpperCase();
            // Count tracks on side A (positions starting with "A" or "1")
            if (position.startsWith("A") || position.startsWith("1")) {
                duration += track.getDurationSeconds();
            }
        }
        return duration;
    }

    /**
     * Manually flip the record to side B
     * Adjusts the start time to skip side A
     */
    public void flipToSideB() {
        int sideADuration = getSideADurationSeconds();
        if (sideADuration > 0) {
            // Adjust start time backwards by side A duration
            // This makes elapsed time jump to the start of side B
            startTimeMillis = System.currentTimeMillis() - (sideADuration * 1000L);
        }
    }

    /**
     * Check if we should show the flip record button
     * Show it when we're near the end of side A (within last 30 seconds)
     * @return true if flip button should be shown
     */
    public boolean shouldShowFlipButton() {
        if (!isOnSideA() || tracklist == null || tracklist.isEmpty()) {
            return false;
        }

        long elapsedSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
        int sideADuration = getSideADurationSeconds();

        // Show button when within last 30 seconds of side A, or any time on side A
        return sideADuration > 0 && elapsedSeconds < sideADuration;
    }

    @Override
    public String toString() {
        return "RecognitionResult{" +
                "artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", title='" + title + '\'' +
                ", releaseId='" + releaseId + '\'' +
                ", year=" + year +
                '}';
    }
}
