package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.PathMetadataStore;

import java.awt.*;
import java.nio.file.Path;
import java.util.UUID;

public class JIPipeGraphEdgeControlPoint {
    private PathMetadataStore metadataStore = new PathMetadataStore();

    public JIPipeGraphEdgeControlPoint() {
    }

    @JsonGetter("metadata")
    public PathMetadataStore getMetadataStore() {
        return metadataStore;
    }

    @JsonSetter("metadata")
    public void setMetadataStore(PathMetadataStore metadataStore) {
        this.metadataStore = metadataStore;
    }

    /**
     * Sets the UI location of this control point within the specified compartment
     *
     * @param compartment The compartment ID. Set to empty string for no compartment.
     * @param location    The UI location. Can be null to reset the location
     */
    public void setLocationWithin(String compartment, Point location) {
        compartment = StringUtils.orElse(compartment, "_");
        metadataStore.put(Path.of("location", compartment, "x"), location.x);
        metadataStore.put(Path.of("location", compartment, "y"), location.y);
    }

    /**
     * Sets the UI location of this control point within the specified compartment
     *
     * @param compartment The compartment ID
     * @param location    The UI location. Can be null to reset the location
     */
    public void setLocationWithin(UUID compartment, Point location) {
        setLocationWithin(StringUtils.nullToEmpty(compartment), location);
    }
}
