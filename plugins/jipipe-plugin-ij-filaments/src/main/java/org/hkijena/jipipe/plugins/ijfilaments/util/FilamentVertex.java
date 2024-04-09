/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.vecmath.Vector3d;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FilamentVertex {

    private UUID uuid = UUID.randomUUID();

    private Point3d spatialLocation = new Point3d();

    private NonSpatialPoint3d nonSpatialLocation = new NonSpatialPoint3d();

    private Quantity physicalVoxelSizeX = new Quantity(1, "pixel");

    private Quantity physicalVoxelSizeY = new Quantity(1, "pixel");

    private Quantity physicalVoxelSizeZ = new Quantity(1, "pixel");

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
        this.physicalVoxelSizeX = new Quantity(other.physicalVoxelSizeX);
        this.physicalVoxelSizeY = new Quantity(other.physicalVoxelSizeY);
        this.physicalVoxelSizeZ = new Quantity(other.physicalVoxelSizeZ);
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

    @JsonGetter("physical-voxel-size-x")
    public Quantity getPhysicalVoxelSizeX() {
        return physicalVoxelSizeX;
    }

    @JsonSetter("physical-voxel-size-x")
    public void setPhysicalVoxelSizeX(Quantity physicalVoxelSizeX) {
        this.physicalVoxelSizeX = physicalVoxelSizeX;
    }

    @JsonGetter("physical-voxel-size-y")
    public Quantity getPhysicalVoxelSizeY() {
        return physicalVoxelSizeY;
    }

    @JsonSetter("physical-voxel-size-y")
    public void setPhysicalVoxelSizeY(Quantity physicalVoxelSizeY) {
        this.physicalVoxelSizeY = physicalVoxelSizeY;
    }

    @JsonGetter("physical-voxel-size-z")
    public Quantity getPhysicalVoxelSizeZ() {
        return physicalVoxelSizeZ;
    }

    @JsonSetter("physical-voxel-size-z")
    public void setPhysicalVoxelSizeZ(Quantity physicalVoxelSizeZ) {
        this.physicalVoxelSizeZ = physicalVoxelSizeZ;
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
        if (is2D()) {
            return getSpatialLocation().getZ();
        } else {
            return useThickness ? getSpatialLocation().getZ() - radius : getSpatialLocation().getZ();
        }
    }

    public double getZMax(boolean useThickness) {
        if (is2D()) {
            return getSpatialLocation().getZ();
        } else {
            return useThickness ? getSpatialLocation().getZ() + radius : getSpatialLocation().getZ();
        }
    }

    public boolean is2D() {
        return physicalVoxelSizeZ.getValue() <= 0;
    }

    /**
     * Finds the common unit within the vertex.
     * Returns 'pixels' if units are inconsistent
     *
     * @return the consensus unit
     */
    public String getConsensusPhysicalSizeUnit() {
        if (is2D()) {
            if (Objects.equals(physicalVoxelSizeX.getUnit(), physicalVoxelSizeY.getUnit())) {
                return physicalVoxelSizeX.getUnit();
            } else {
                return Quantity.UNIT_PIXELS;
            }
        } else {
            if (Objects.equals(physicalVoxelSizeX.getUnit(), physicalVoxelSizeY.getUnit())
                    && Objects.equals(physicalVoxelSizeY.getUnit(), physicalVoxelSizeZ.getUnit())) {
                return physicalVoxelSizeX.getUnit();
            } else {
                return Quantity.UNIT_PIXELS;
            }
        }
    }

    public Vector3d getSpatialLocationInUnit(String unit) {
        return spatialLocation.pixelsToUnit(physicalVoxelSizeX, physicalVoxelSizeY, physicalVoxelSizeZ, unit);
    }

    /**
     * Converts the radius into units and returns the maximum value
     *
     * @param unit the unit
     * @return the maximum value
     */
    public double getMaxRadiusInUnit(String unit) {
        if (is2D()) {
            double rx = physicalVoxelSizeX.convertTo(unit).getValue() * radius;
            double ry = physicalVoxelSizeY.convertTo(unit).getValue() * radius;
            return Math.max(rx, ry);
        } else {
            double rx = physicalVoxelSizeX.convertTo(unit).getValue() * radius;
            double ry = physicalVoxelSizeY.convertTo(unit).getValue() * radius;
            double rz = physicalVoxelSizeZ.convertTo(unit).getValue() * radius;
            return Math.max(rx, Math.max(ry, rz));
        }
    }
}
