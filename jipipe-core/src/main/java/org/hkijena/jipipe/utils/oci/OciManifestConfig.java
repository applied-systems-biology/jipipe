package org.hkijena.jipipe.utils.oci;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OciManifestConfig {
    @JsonProperty("mediaType")
    private String mediaType;

    @JsonProperty("digest")
    private String digest;

    @JsonProperty("size")
    private long size;

    @JsonProperty("data")
    private String data;

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
