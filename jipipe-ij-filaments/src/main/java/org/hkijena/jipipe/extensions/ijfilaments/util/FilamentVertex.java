package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class FilamentVertex {

    private FilamentLocation centroid = new FilamentLocation();

    public FilamentVertex() {

    }

    public FilamentVertex(FilamentVertex other) {
        this.centroid = new FilamentLocation(other.centroid);
    }

    @JsonGetter("centroid")
    public FilamentLocation getCentroid() {
        return centroid;
    }

    @JsonSetter("centroid")
    public void setCentroid(FilamentLocation centroid) {
        this.centroid = centroid;
    }
}
