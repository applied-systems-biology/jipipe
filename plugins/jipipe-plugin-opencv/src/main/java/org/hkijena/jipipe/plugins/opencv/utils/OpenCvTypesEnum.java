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

public enum OpenCvTypesEnum {
    CV_8U(opencv_core.CV_8U),
    CV_8UC2(opencv_core.CV_8UC2),
    CV_8UC3(opencv_core.CV_8UC3),
    CV_8UC4(opencv_core.CV_8UC(4)),
    CV_8UC5(opencv_core.CV_8UC(5)),
    CV_8S(opencv_core.CV_8S),
    CV_8SC2(opencv_core.CV_8SC2),
    CV_8SC3(opencv_core.CV_8SC3),
    CV_8SC4(opencv_core.CV_8SC(4)),
    CV_8SC5(opencv_core.CV_8SC(5)),
    CV_16S(opencv_core.CV_16S),
    CV_16SC2(opencv_core.CV_16SC2),
    CV_16SC3(opencv_core.CV_16SC3),
    CV_16SC4(opencv_core.CV_16SC(4)),
    CV_16SC5(opencv_core.CV_16SC(5)),
    CV_16U(opencv_core.CV_16U),
    CV_16UC2(opencv_core.CV_16UC2),
    CV_16UC3(opencv_core.CV_16UC3),
    CV_16UC4(opencv_core.CV_16UC(4)),
    CV_16UC5(opencv_core.CV_16UC(5)),
    CV_32S(opencv_core.CV_32S),
    CV_32SC2(opencv_core.CV_32SC2),
    CV_32SC3(opencv_core.CV_32SC3),
    CV_32SC4(opencv_core.CV_32SC(4)),
    CV_32SC5(opencv_core.CV_32SC(5)),
    CV_32F(opencv_core.CV_32F),
    CV_32FC2(opencv_core.CV_32FC2),
    CV_32FC3(opencv_core.CV_32FC3),
    CV_32FC4(opencv_core.CV_32FC(4)),
    CV_32FC5(opencv_core.CV_32FC(5));

    private final int nativeValue;

    OpenCvTypesEnum(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
