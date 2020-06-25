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

package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings.
 * This contains individually adresses the columns
 */
public enum MeasurementColumn {
    Area(1, "Area"),
    PixelValueMean(2, "Mean"),
    PixelValueStandardDeviation(4, "StdDev"),
    PixelValueModal(8, "Mode"),
    PixelValueMin(16, "Min"),
    PixelValueMax(16, "Max"),
    CentroidX(32, "X"),
    CentroidY(32, "Y"),
    CenterOfMassX(64, "XM"),
    CenterOfMassY(64, "YM"),
    Perimeter(128, "Perim."),
    BoundingRectangleX(512, "BX"),
    BoundingRectangleY(512, "BY"),
    BoundingRectangleWidth(512, "Width"),
    BoundingRectangleHeight(512, "Height"),
    FittedEllipseMajor(2048, "Major"),
    FittedEllipseMinor(2048, "Minor"),
    FittedEllipseAngles(2048, "Angle"),
    ShapeDescriptorCircularity(8192, "Circ."),
    ShapeDescriptorAspectRatio(8192, "AR"),
    ShapeDescriptorRoundness(8192, "Round"),
    ShapeDescriptorSolidity(8192, "Solidity"),
    FeretDiameter(16384, "Feret"),
    FeretAngle(16384, "FeretAngle"),
    MinFeret(16384, "MinFeret"),
    FeretStartingCoordinateX(16384, "FeretX"),
    FeretStartingCoordinateY(16384, "FeretY"),
    IntegratedDensity(0x8000, "IntDen"),
    RawIntegratedDensity(0x8000, "RawIntDen"),
    PixelValueMedian(0x10000, "Median"),
    PixelValueSkewness(0x20000, "Skew"),
    PixelValueKurtosis(0x40000, "Kurt"),
    AreaFraction(0x80000, "%Area"),
    StackPositionSlice(0x100000, "Slice"),
    StackPositionFrame(0x100000, "Frame"),
    StackPositionChannel(0x100000, "Ch");

    private final int nativeValue;
    private final String columnName;

    MeasurementColumn(int nativeValue, String columnName) {
        this.nativeValue = nativeValue;
        this.columnName = columnName;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public String getColumnName() {
        return columnName;
    }
}
