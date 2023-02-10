package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@JsonSerialize
public class FilamentEdge {

    private UUID uuid = UUID.randomUUID();

    private Color color = new Color(0x3584E4);

    private Map<String, String> metadata = new HashMap<>();

    @JsonGetter("metadata")
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @JsonGetter("uuid")
    public UUID getUuid() {
        return uuid;
    }

    @JsonSetter("uuid")
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @JsonGetter("color")
    public Color getColor() {
        return color;
    }

    @JsonSetter("color")
    public void setColor(Color color) {
        this.color = color;
    }

    public FilamentEdge() {

    }

    public FilamentEdge(FilamentEdge other) {
    }
}
