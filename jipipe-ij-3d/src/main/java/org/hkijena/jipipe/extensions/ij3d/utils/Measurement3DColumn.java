/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.ij3d.utils;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings.
 * This contains individually addressed columns
 */
public enum Measurement3DColumn {
    Index(1, "Index", "Index", "The numeric ROI index"),
    Name(2, "Name", "Name", "The name of the ROI"),
    Comment(4, "Comment", "Comment", "The comment string of the ROI"),
    Area(8, "Area", "Area", "The area of the surface"),
    Volume(16, "Volume", "Volume", "The volume of the ROI"),
    CenterX(32, "CenterX", "Center (X)", "The X location of the ROI's center"),
    CenterY(32, "CenterY", "Center (Y)", "The Y location of the ROI's center"),
    CenterZ(32, "CenterZ", "Center (Z)", "The Z location of the ROI's center"),
    CenterPixelValue(32, "CenterPixelValue", "Center (value)", "The pixel value of the ROI's center (or NaN)"),
    ShapeMeasurementCompactness(64, "Compactness", "Compactness", "The compactness the ROI"),
    ShapeMeasurementSphericity(64, "Sphericity", "Sphericity", "The sphericity the ROI"),
    ShapeMeasurementFeret(64, "Feret", "Feret", "The feret diameter of the object (unit)"),
    ShapeMeasurementMainElongation(64, "MainElongation", "Main elongation", "The main elongation the ROI"),
    ShapeMeasurementMedianElongation(64, "MedianElongation", "Median elongation", "The flatness of the object"),
    ShapeMeasurementRatioBox(64, "RatioBox", "Ratio to box", "The ration between volume and volume of the bounding box (in units)"),
    ShapeMeasurementRatioEllipsoid(64, "RatioEllipsoid", "Ratio to ellipsoid", "The ration between volume and volume of the ellipsoid (in units)"),
    DistCenterMax(128, "DistCenterMax", "Distance to center (max)", "The maximum distance between center and contours"),
    DistCenterMean(128, "DistCenterMean", "Distance to center (mean)", "The average distance between center and contours"),
    DistCenterSigma(128, "DistCenterSigma", "Distance to center (sigma)", "The sigma value for distances between center and contours"),
    PixelValueMax(256, "PixelValueMax", "Pixel value (max)", "The maximum pixel value inside the object (or NaN)"),
    PixelValueMin(256, "PixelValueMin", "Pixel value (min)", "The minimum pixel value inside the object (or NaN)"),
    PixelValueMean(256, "PixelValueMean", "Pixel value (mean)", "The average pixel value inside the object (or NaN)"),
    PixelValueMedian(256, "PixelValueMedian", "Pixel value (median)", "The median pixel value inside the object (or NaN)"),
    PixelValueStdDev(256, "PixelValueStdDev", "Pixel value (std dev)", "The standard deviation of the pixel values inside the object (or NaN)"),
    PixelValueMode(256, "PixelValueMode", "Pixel value (mode)", "The mode of the pixel values inside the object (or NaN)"),
    PixelValueModeNonZero(256, "PixelValueModeNonZero", "Pixel value (mode, non-zero)", "The mode of the pixel values (excluding zero values) inside the object (or NaN)"),
    PixelValueIntDen(256, "PixelValueIntDen", "Pixel value (integrated density)", "The integrated density of the pixel values inside the object (or NaN)"),

    ContourPixelValueMean(512, "ContourPixelValueMean", "Pixel value (contour, mean)", "The average pixel value on the contour of the object (or NaN)"),
    BoundingBoxMinX(1024, "BoundingBoxMinX", "Bounding box (min X)", "The minimum X of the bounding box around the object"),
    BoundingBoxMinY(1024, "BoundingBoxMinY", "Bounding box (min Y)", "The minimum Y of the bounding box around the object"),
    BoundingBoxMinZ(1024, "BoundingBoxMinZ", "Bounding box (min Z)", "The minimum Z of the bounding box around the object"),

    BoundingBoxMaxX(1024, "BoundingBoxMaxX", "Bounding box (max X)", "The maximum X of the bounding box around the object"),
    BoundingBoxMaxY(1024, "BoundingBoxMaxY", "Bounding box (max Y)", "The maximum Y of the bounding box around the object"),
    BoundingBoxMaxZ(1024, "BoundingBoxMaxZ", "Bounding box (max Z)", "The maximum Z of the bounding box around the object"),
    ResolutionXY(2048, "ResolutionXY", "Physical voxel size (X/Y)", "The physical size of a voxel in the X and Y axes"),
    ResolutionZ(2048, "ResolutionZ", "Physical voxel size (Z)", "The physical size of a voxel in the Z axis"),
    ResolutionUnit(2048, "ResolutionUnit", "Physical voxel size (unit)", "The unit of the physical size"),
    MassCenterX(4096, "MassCenterX", "Center of mass (X)", "The X coordinate of the center of Mass (or NaN)"),
    MassCenterY(4096, "MassCenterY", "Center of mass (Y)", "The Y coordinate of the center of Mass (or NaN)"),
    MassCenterZ(4096, "MassCenterZ", "Center of mass (Z)", "The Z coordinate of the center of Mass (or NaN)"),

    LocationChannel(8192, "Channel", "Channel", "The channel of the ROI (0 = all channels)"),
    LocationFrame(8192, "Frame", "Frame", "The frame of the ROI (0 = all frames)"),
    FillColor(16384, "FillColor", "Fill color", "The fill color (HEX string)")
    ;

    private final int nativeValue;
    private final String columnName;
    private final String name;
    private final String description;

    Measurement3DColumn(int nativeValue, String columnName, String name, String description) {
        this.nativeValue = nativeValue;
        this.columnName = columnName;
        this.name = name;
        this.description = description;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
