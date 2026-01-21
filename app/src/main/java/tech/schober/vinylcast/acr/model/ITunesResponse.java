package tech.schober.vinylcast.acr.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response from iTunes Search API
 */
public class ITunesResponse {
    @SerializedName("resultCount")
    private int resultCount;

    @SerializedName("results")
    private List<Result> results;

    public int getResultCount() {
        return resultCount;
    }

    public List<Result> getResults() {
        return results;
    }

    public static class Result {
        @SerializedName("artistName")
        private String artistName;

        @SerializedName("collectionName")
        private String collectionName;

        @SerializedName("trackName")
        private String trackName;

        @SerializedName("artworkUrl100")
        private String artworkUrl100;

        @SerializedName("artworkUrl60")
        private String artworkUrl60;

        public String getArtistName() {
            return artistName;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getTrackName() {
            return trackName;
        }

        public String getArtworkUrl100() {
            return artworkUrl100;
        }

        public String getArtworkUrl60() {
            return artworkUrl60;
        }

        /**
         * Get high resolution artwork URL by replacing size in URL
         * iTunes allows requesting larger sizes by changing the size parameter
         * @param size Desired size (e.g., 600, 1200, 3000, 5000)
         * @return High resolution artwork URL
         */
        public String getArtworkUrl(int size) {
            if (artworkUrl100 != null) {
                return artworkUrl100.replace("100x100", size + "x" + size);
            }
            return null;
        }
    }
}
