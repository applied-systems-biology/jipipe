package org.hkijena.jipipe.utils.oci;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class OciManifest {

    @JsonProperty("schemaVersion")
    private int schemaVersion;

    @JsonProperty("mediaType")
    private String mediaType;

    @JsonProperty("artifactType")
    private String artifactType;

    @JsonProperty("config")
    private OciManifestConfig config;

    @JsonProperty("layers")
    private List<OciManifestLayer> layers;

    @JsonProperty("annotations")
    private Map<String, String> annotations;

    public int getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(int schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public OciManifestConfig getConfig() { return config; }
    public void setConfig(OciManifestConfig config) { this.config = config; }

    public List<OciManifestLayer> getLayers() { return layers; }
    public void setLayers(List<OciManifestLayer> layers) { this.layers = layers; }

    public Map<String, String> getAnnotations() { return annotations; }
    public void setAnnotations(Map<String, String> annotations) { this.annotations = annotations; }
}

