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

package org.hkijena.jipipe.plugins.opencv.utils;

import org.bytedeco.opencv.global.opencv_core;

public enum OpenCvDepth {
    CV_8U(opencv_core.CV_8U),
    CV_8S(opencv_core.CV_8S),
    CV_16S(opencv_core.CV_16S),
    CV_16U(opencv_core.CV_16U),
    CV_32S(opencv_core.CV_32S),
    CV_32F(opencv_core.CV_32F);

    private final int nativeValue;

    OpenCvDepth(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
