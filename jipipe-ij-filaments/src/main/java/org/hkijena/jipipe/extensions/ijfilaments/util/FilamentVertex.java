package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.utils.StringUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FilamentVertex {

    private UUID uuid = UUID.randomUUID();

    private Point3d spatialLocation = new Point3d();

    private NonSpatialPoint3d nonSpatialLocation = new NonSpatialPoint3d();

    private double radius = 1;

    private double value = 0;

    private Color color = new Color(0xE5A50A);

    private Map<String, String> metadata = new HashMap<>();

    public FilamentVertex() {

    }

    public FilamentVertex(FilamentVertex other) {
        this.spatialLocation = new Point3d(other.spatialLocation);
        this.nonSpatialLocation = new NonSpatialPoint3d(other.nonSpatialLocation);
        this.radius = other.radius;
        this.value = other.value;
        this.metadata = new HashMap<>(other.metadata);
        this.color = other.color;
    }

    @JsonGetter("spatial-location")
    public Point3d getSpatialLocation() {
        return spatialLocation;
    }

    @JsonSetter("spatial-location")
    public void setSpatialLocation(Point3d spatialLocation) {
        this.spatialLocation = spatialLocation;
    }

    @JsonGetter("non-spatial-location")
    public NonSpatialPoint3d getNonSpatialLocation() {
        return nonSpatialLocation;
    }

    @JsonSetter("non-spatial-location")
    public void setNonSpatialLocation(NonSpatialPoint3d nonSpatialLocation) {
        this.nonSpatialLocation = nonSpatialLocation;
    }

    @JsonGetter("value")
    public double getValue() {
        return value;
    }

    @JsonSetter("value")
    public void setValue(double value) {
        this.value = value;
    }

    @JsonGetter("radius")
    public double getRadius() {
        return radius;
    }

    @JsonSetter("radius")
    public void setRadius(double radius) {
        this.radius = radius;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilamentVertex vertex = (FilamentVertex) o;
        return uuid.equals(vertex.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, StringUtils.nullToEmpty(value));
    }

    public double getXMin(boolean useThickness) {
        return useThickness ? getSpatialLocation().getX() - radius : getSpatialLocation().getX();
    }

    public double getXMax(boolean useThickness) {
        return useThickness ? getSpatialLocation().getX() + radius : getSpatialLocation().getX();
    }

    public double getYMin(boolean useThickness) {
        return useThickness ? getSpatialLocation().getY() - radius : getSpatialLocation().getY();
    }

    public double getYMax(boolean useThickness) {
        return useThickness ? getSpatialLocation().getY() + radius : getSpatialLocation().getY();
    }

    public double getZMin(boolean useThickness) {
        return useThickness ? getSpatialLocation().getZ() - radius : getSpatialLocation().getZ();
    }

    public double getZMax(boolean useThickness) {
        return useThickness ? getSpatialLocation().getZ() + radius : getSpatialLocation().getZ();
    }
}
