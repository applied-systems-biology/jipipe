package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.Map;

public class FilamentVertex {

    private FilamentLocation centroid = new FilamentLocation();

    private double thickness = 1;

    private Map<String, String> metadata = new HashMap<>();

    public FilamentVertex() {

    }

    public FilamentVertex(FilamentVertex other) {
        this.centroid = new FilamentLocation(other.centroid);
        this.thickness = other.thickness;
        this.metadata = new HashMap<>(other.metadata);
    }

    @JsonGetter("centroid")
    public FilamentLocation getCentroid() {
        return centroid;
    }

    @JsonSetter("centroid")
    public void setCentroid(FilamentLocation centroid) {
        this.centroid = centroid;
    }

    @JsonGetter("thickness")
    public double getThickness() {
        return thickness;
    }

    @JsonSetter("thickness")
    public void setThickness(double thickness) {
        this.thickness = thickness;
    }

    @JsonGetter("metadata")
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
