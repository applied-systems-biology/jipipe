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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.measure;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings.
 * This contains individually adresses the columns
 */
public enum MeasurementColumn {
    Area(1, "Area", "Area", "Area in pixels"),
    PixelValueMean(2, "Mean", "Mean pixel value", "Mean of the grayscale pixel value"),
    PixelValueStandardDeviation(4, "StdDev", "Pixel value standard deviation", "Standard deviation of the grayscale pixe lvalue"),
    PixelValueModal(8, "Mode", "Modal pixel value", "Most frequently occurring gray value within the selection"),
    PixelValueMin(16, "Min", "Minimum pixel value", "Minimum grayscale pixel value"),
    PixelValueMax(16, "Max", "Maximum pixel value", "Maximum grayscale pixel value"),
    CentroidX(32, "X", "Centroid X", "The X coordinate of the centroid"),
    CentroidY(32, "Y", "Centroid Y", "The Y coordinate of the centroid"),
    CenterOfMassX(64, "XM", "Center of mass X", "The X coordinate of the center of mass"),
    CenterOfMassY(64, "YM", "Center of mass Y", "The Y coordinate of the center of mass"),
    Perimeter(128, "Perim.", "Perimeter", "The length of the outside boundary of the selection"),

    BoundingRectangleX(512, "BX", "Bounding rectangle X", "X coordinate of the bounding rectangle"),
    BoundingRectangleY(512, "BY", "Bounding rectangle Y", "Y coordinate of the bounding rectangle"),
    BoundingRectangleWidth(512, "Width", "Bounding rectangle width", "Width of the bounding rectangle"),
    BoundingRectangleHeight(512, "Height", "Bounding rectangle height", "Height of the bounding rectangle"),
    RotatedBoundingRectangleWidth(512, "RBWidth", "Minimum bounding rectangle width", "Width of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleHeight(512, "RBHeight", "Minimum bounding rectangle height", "Height of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleX1(512, "RBX1", "Minimum bounding rectangle X1", "X of the first point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleX2(512, "RBX2", "Minimum bounding rectangle X2", "X of the second point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleX3(512, "RBX3", "Minimum bounding rectangle X3", "X of the third point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleX4(512, "RBX4", "Minimum bounding rectangle X4", "X of the fourth point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleY1(512, "RBY1", "Minimum bounding rectangle Y1", "Y of the first point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleY2(512, "RBY2", "Minimum bounding rectangle Y2", "Y of the second point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleY3(512, "RBY3", "Minimum bounding rectangle Y3", "Y of the third point of the minimum (rotated) bounding rectangle"),
    RotatedBoundingRectangleY4(512, "RBY4", "Minimum bounding rectangle Y4", "Y of the fourth point of the minimum (rotated) bounding rectangle"),

    FittedEllipseMajor(2048, "Major", "Fitted ellipse major", "Major axis of the fitted ellipse"),
    FittedEllipseMinor(2048, "Minor", "Fitted ellipse minor", "Minor axis of the fitted ellipse"),
    FittedEllipseAngles(2048, "Angle", "Fitted ellipse angle", "Angle of the fitted ellipse (0-180)"),
    ShapeDescriptorCircularity(8192, "Circ.", "Circularity", "4π*area/perimeter^2. A value of 1.0 indicates a perfect circle."),

    ShapeDescriptorAspectRatio(8192, "AR", "Aspect ratio (fitted ellipse)", "major_axis/minor_axis of the ellipse fitted to the ROI (measurement inherited from ImageJ)"),
    ShapeDescriptorRectangleAspectRatio(8192, "rAR", "Aspect ratio (fitted rectangle)", "fits a rotated rectangle around the ROI and calculates the aspect ratio (major_axis/minor_axis) of this rectangle"),

    ShapeDescriptorRoundness(8192, "Round", "Roundness", "4*area/(π*major_axis^2), or the inverse of the aspect ratio."),
    ShapeDescriptorSolidity(8192, "Solidity", "Solidity", "area/convex area"),
    FeretDiameter(16384, "Feret", "Feret diameter", "The longest distance between any two points along the selection boundary, also known as maximum caliper"),
    FeretAngle(16384, "FeretAngle", "Feret angle", "The angle between the Feret's diameter and a line parallel to the x-axis of the image"),
    MinFeret(16384, "MinFeret", "Minimum calipher diameter", "The smallest distance between any two points along the selection boundary"),
    FeretStartingCoordinateX(16384, "FeretX", "Feret starting X", "Starting X coordinate of the Feret diameter line"),
    FeretStartingCoordinateY(16384, "FeretY", "Feret starting Y", "Starting Y coordinate of the Feret diameter line"),
    IntegratedDensity(0x8000, "IntDen", "Integrated density", "The product of Area and Mean Gray Value"),
    RawIntegratedDensity(0x8000, "RawIntDen", "Raw integrated density", "The sum of the values of the pixels in the image or selection"),
    PixelValueMedian(0x10000, "Median", "Median pixel value", "The median value of the pixels in the image or selection"),
    PixelValueSkewness(0x20000, "Skew", "Pixel value skewness", "The third order moment about the mean of grayscale pixel values"),
    PixelValueKurtosis(0x40000, "Kurt", "Pixel value kurosis", "The fourth order moment about the greyscale pixel value mean"),
    AreaFraction(0x80000, "%Area", "Area fraction", "The percentage of non-zero pixels"),
    StackPositionSlice(0x100000, "Slice", "Stack position: Slice", "The current position within the stack or hyperstack"),
    StackPositionFrame(0x100000, "Frame", "Stack position: Frame", "The current position within the stack or hyperstack"),
    StackPositionChannel(0x100000, "Ch", "Stack position: Channel", "The current position within the stack or hyperstack");

    private final int nativeValue;
    private final String columnName;
    private final String name;
    private final String description;

    MeasurementColumn(int nativeValue, String columnName, String name, String description) {
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
