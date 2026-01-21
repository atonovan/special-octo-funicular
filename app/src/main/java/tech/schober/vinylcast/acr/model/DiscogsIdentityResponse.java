package tech.schober.vinylcast.acr.model;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Discogs identity API
 */
public class DiscogsIdentityResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("username")
    private String username;

    @SerializedName("resource_url")
    private String resourceUrl;

    @SerializedName("consumer_name")
    private String consumerName;

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public String getConsumerName() {
        return consumerName;
    }
}
