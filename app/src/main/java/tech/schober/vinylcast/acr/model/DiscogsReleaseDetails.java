package tech.schober.vinylcast.acr.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Detailed release information from Discogs API
 * Used for fetching full album data including tracklist and personnel
 */
public class DiscogsReleaseDetails {
    @SerializedName("id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("year")
    private int year;

    @SerializedName("genres")
    private List<String> genres;

    @SerializedName("styles")
    private List<String> styles;

    @SerializedName("tracklist")
    private List<Track> tracklist;

    @SerializedName("artists")
    private List<Artist> artists;

    @SerializedName("extraartists")
    private List<Artist> extraArtists;

    @SerializedName("images")
    private List<Image> images;

    @SerializedName("labels")
    private List<Label> labels;

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public List<String> getGenres() {
        return genres;
    }

    public List<String> getStyles() {
        return styles;
    }

    public List<Track> getTracklist() {
        return tracklist;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public List<Artist> getExtraArtists() {
        return extraArtists;
    }

    public List<Image> getImages() {
        return images;
    }

    public List<Label> getLabels() {
        return labels;
    }

    /**
     * Get key personnel (producers, engineers, etc.)
     * Filters extraartists for notable roles
     */
    public String getKeyPersonnel() {
        if (extraArtists == null || extraArtists.isEmpty()) {
            return null;
        }

        StringBuilder personnel = new StringBuilder();
        int count = 0;
        for (Artist artist : extraArtists) {
            // Include producers, engineers, and other key roles
            String role = artist.getRole();
            if (role != null && (role.contains("Producer") ||
                                 role.contains("Engineer") ||
                                 role.contains("Mixed By") ||
                                 role.contains("Mastered By"))) {
                if (count > 0) {
                    personnel.append(", ");
                }
                personnel.append(artist.getName());
                if (role != null && !role.isEmpty()) {
                    personnel.append(" (").append(role).append(")");
                }
                count++;
                if (count >= 3) { // Limit to top 3
                    break;
                }
            }
        }
        return personnel.length() > 0 ? personnel.toString() : null;
    }

    /**
     * Get best quality cover image URL
     */
    public String getBestImageUrl() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        // Return first primary image or just first image
        for (Image image : images) {
            if (image.getType() != null && image.getType().equals("primary")) {
                return image.getUri();
            }
        }
        return images.get(0).getUri();
    }

    public static class Track {
        @SerializedName("position")
        private String position;

        @SerializedName("type_")
        private String type;

        @SerializedName("title")
        private String title;

        @SerializedName("duration")
        private String duration;

        public String getPosition() {
            return position;
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getDuration() {
            return duration;
        }

        /**
         * Parse duration string (e.g., "3:45") to seconds
         */
        public int getDurationSeconds() {
            if (duration == null || duration.isEmpty()) {
                return 0;
            }
            try {
                String[] parts = duration.split(":");
                if (parts.length == 2) {
                    return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
                } else if (parts.length == 3) {
                    return Integer.parseInt(parts[0]) * 3600 +
                           Integer.parseInt(parts[1]) * 60 +
                           Integer.parseInt(parts[2]);
                }
            } catch (NumberFormatException e) {
                return 0;
            }
            return 0;
        }
    }

    public static class Artist {
        @SerializedName("name")
        private String name;

        @SerializedName("anv")
        private String anv;

        @SerializedName("join")
        private String join;

        @SerializedName("role")
        private String role;

        @SerializedName("tracks")
        private String tracks;

        public String getName() {
            return name;
        }

        public String getAnv() {
            return anv;
        }

        public String getJoin() {
            return join;
        }

        public String getRole() {
            return role;
        }

        public String getTracks() {
            return tracks;
        }
    }

    public static class Image {
        @SerializedName("type")
        private String type;

        @SerializedName("uri")
        private String uri;

        @SerializedName("resource_url")
        private String resourceUrl;

        @SerializedName("uri150")
        private String uri150;

        @SerializedName("width")
        private int width;

        @SerializedName("height")
        private int height;

        public String getType() {
            return type;
        }

        public String getUri() {
            return uri;
        }

        public String getResourceUrl() {
            return resourceUrl;
        }

        public String getUri150() {
            return uri150;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public static class Label {
        @SerializedName("name")
        private String name;

        @SerializedName("catno")
        private String catno;

        public String getName() {
            return name;
        }

        public String getCatno() {
            return catno;
        }
    }
}
