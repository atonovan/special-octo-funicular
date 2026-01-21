package tech.schober.vinylcast.acr.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response from Discogs collection API
 */
public class DiscogsCollectionResponse {
    @SerializedName("pagination")
    private Pagination pagination;

    @SerializedName("releases")
    private List<CollectionRelease> releases;

    public Pagination getPagination() {
        return pagination;
    }

    public List<CollectionRelease> getReleases() {
        return releases;
    }

    public static class Pagination {
        @SerializedName("page")
        private int page;

        @SerializedName("pages")
        private int pages;

        @SerializedName("per_page")
        private int perPage;

        @SerializedName("items")
        private int items;

        public int getPage() {
            return page;
        }

        public int getPages() {
            return pages;
        }

        public int getPerPage() {
            return perPage;
        }

        public int getItems() {
            return items;
        }
    }

    public static class CollectionRelease {
        @SerializedName("id")
        private long id;

        @SerializedName("basic_information")
        private BasicInformation basicInformation;

        public long getId() {
            return id;
        }

        public BasicInformation getBasicInformation() {
            return basicInformation;
        }
    }

    public static class BasicInformation {
        @SerializedName("id")
        private long id;

        @SerializedName("title")
        private String title;

        @SerializedName("year")
        private int year;

        @SerializedName("formats")
        private List<Format> formats;

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public int getYear() {
            return year;
        }

        public List<Format> getFormats() {
            return formats;
        }
    }

    public static class Format {
        @SerializedName("name")
        private String name;

        @SerializedName("qty")
        private String qty;

        @SerializedName("descriptions")
        private List<String> descriptions;

        public String getName() {
            return name;
        }

        public String getQty() {
            return qty;
        }

        public List<String> getDescriptions() {
            return descriptions;
        }
    }
}
