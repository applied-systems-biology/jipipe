package org.hkijena.acaq5.extensions.imagejalgorithms.ij1.measure;

/**
 * Measurements defined by {@link ij.measure.Measurements}.
 * This only includes the measurements that actually generate columns - not measurement settings
 */
public enum Measurement {
    Area(1),
    PixelValueMean(2),
    PixelValueStandardDeviation(4),
    PixelValueModal(8),
    PixelValueMinMax(16),
    Centroid(32),
    CenterOfMass(64),
    Perimeter(128),
    BoundingRectangle(512),
    FitEllipse(2048),
    ShapeDescriptors(8192),
    FeretDiameter(16384),
    IntegratedDensity(0x8000),
    PixelValueMedian(0x10000),
    PixelValueSkewness(0x20000),
    PixelValueKurtosis(0x40000),
    AreaFraction(0x80000),
    StackPosition(0x100000);

    private final int nativeValue;

    Measurement(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
