package org.hkijena.acaq5.extensions.imagejalgorithms.ij1;

import ij.process.ImageProcessor;

/**
 * Available interpolation methods
 */
public enum InterpolationMethod {
    None(ImageProcessor.NONE),
    Bilinear(ImageProcessor.BILINEAR),
    Bicubic(ImageProcessor.BICUBIC);

    private final int nativeValue;

    InterpolationMethod(int nativeValue) {
        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
