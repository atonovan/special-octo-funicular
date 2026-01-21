package tech.schober.vinylcast.acr.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response from Discogs API
 */
public class DiscogsResponse {
    @SerializedName("results")
    private List<Result> results;

    public List<Result> getResults() {
        return results;
    }

    public static class Result {
        @SerializedName("id")
        private long id;

        @SerializedName("title")
        private String title;

        @SerializedName("year")
        private String year;

        @SerializedName("format")
        private List<String> format;

        @SerializedName("label")
        private List<String> label;

        @SerializedName("country")
        private String country;

        @SerializedName("genre")
        private List<String> genre;

        @SerializedName("style")
        private List<String> style;

        @SerializedName("barcode")
        private List<String> barcode;

        @SerializedName("cover_image")
        private String coverImage;

        @SerializedName("thumb")
        private String thumb;

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getYear() {
            return year;
        }

        public List<String> getFormat() {
            return format;
        }

        public List<String> getLabel() {
            return label;
        }

        public String getCountry() {
            return country;
        }

        public List<String> getGenre() {
            return genre;
        }

        public List<String> getStyle() {
            return style;
        }

        public List<String> getBarcode() {
            return barcode;
        }

        public String getCoverImage() {
            return coverImage;
        }

        public String getThumb() {
            return thumb;
        }

        /**
         * Parse artist and album from title
         * Discogs format is typically "Artist - Album"
         */
        public String getArtist() {
            if (title != null && title.contains(" - ")) {
                return title.split(" - ")[0].trim();
            }
            return null;
        }

        /**
         * Parse album from title
         * Discogs format is typically "Artist - Album"
         */
        public String getAlbum() {
            if (title != null && title.contains(" - ")) {
                String[] parts = title.split(" - ", 2);
                if (parts.length > 1) {
                    return parts[1].trim();
                }
            }
            return title;
        }
    }
}
