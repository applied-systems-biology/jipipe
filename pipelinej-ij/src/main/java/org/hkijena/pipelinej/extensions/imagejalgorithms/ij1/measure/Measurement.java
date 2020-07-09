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

package org.hkijena.pipelinej.extensions.imagejalgorithms.ij1.measure;

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
