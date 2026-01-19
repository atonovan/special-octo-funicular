package tech.schober.vinylcast.acr.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response from AcoustID API
 */
public class AcoustIdResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("results")
    private List<Result> results;

    public String getStatus() {
        return status;
    }

    public List<Result> getResults() {
        return results;
    }

    public static class Result {
        @SerializedName("id")
        private String id;

        @SerializedName("score")
        private double score;

        @SerializedName("recordings")
        private List<Recording> recordings;

        public String getId() {
            return id;
        }

        public double getScore() {
            return score;
        }

        public List<Recording> getRecordings() {
            return recordings;
        }
    }

    public static class Recording {
        @SerializedName("id")
        private String id;

        @SerializedName("title")
        private String title;

        @SerializedName("artists")
        private List<Artist> artists;

        @SerializedName("releases")
        private List<Release> releases;

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public List<Artist> getArtists() {
            return artists;
        }

        public List<Release> getReleases() {
            return releases;
        }
    }

    public static class Artist {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Release {
        @SerializedName("id")
        private String id;

        @SerializedName("title")
        private String title;

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }
}
