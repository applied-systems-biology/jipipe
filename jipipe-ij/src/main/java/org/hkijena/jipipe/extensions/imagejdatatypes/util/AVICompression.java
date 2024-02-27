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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import ij.plugin.filter.AVI_Writer;

/**
 * Wrapper around {@link ij.plugin.filter.AVI_Writer} compression
 */
public enum AVICompression {
    None(AVI_Writer.NO_COMPRESSION),
    JPEG(AVI_Writer.JPEG_COMPRESSION),
    PNG(AVI_Writer.PNG_COMPRESSION);

    private final int nativeValue;

    AVICompression(int nativeValue) {

        this.nativeValue = nativeValue;
    }

    public int getNativeValue() {
        return nativeValue;
    }
}
