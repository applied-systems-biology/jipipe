package org.hkijena.jipipe.plugins.opencv.utils;


import org.bytedeco.javacpp.opencv_core;

public enum OpenCvBorderType {
    Constant(opencv_core.BORDER_CONSTANT),
    Replicate(opencv_core.BORDER_REPLICATE),
    Reflect(opencv_core.BORDER_REFLECT),
    Wrap(opencv_core.BORDER_WRAP),
    Reflect101(opencv_core.BORDER_REFLECT_101),
    Transparent(opencv_core.BORDER_TRANSPARENT),
    Isolated(opencv_core.BORDER_ISOLATED),;

    private final int nativeValue;

    OpenCvBorderType(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
