package tech.schober.vinylcast.acr.model;

import android.graphics.Bitmap;

/**
 * Represents the result of audio recognition
 */
public class RecognitionResult {
    private final String artist;
    private final String album;
    private final String title;
    private final String releaseId;
    private Bitmap albumArtwork;

    public RecognitionResult(String artist, String album, String title, String releaseId) {
        this.artist = artist;
        this.album = album;
        this.title = title;
        this.releaseId = releaseId;
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

    public Bitmap getAlbumArtwork() {
        return albumArtwork;
    }

    public void setAlbumArtwork(Bitmap albumArtwork) {
        this.albumArtwork = albumArtwork;
    }

    @Override
    public String toString() {
        return "RecognitionResult{" +
                "artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", title='" + title + '\'' +
                ", releaseId='" + releaseId + '\'' +
                '}';
    }
}
