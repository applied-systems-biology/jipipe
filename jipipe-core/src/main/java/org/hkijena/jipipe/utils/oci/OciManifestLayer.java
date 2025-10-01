package org.hkijena.jipipe.utils.oci;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class OciManifestLayer {
    @JsonProperty("mediaType")
    private String mediaType;

    @JsonProperty("digest")
    private String digest;

    @JsonProperty("size")
    private long size;

    @JsonProperty("annotations")
    private Map<String, String> annotations;

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

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }
}
