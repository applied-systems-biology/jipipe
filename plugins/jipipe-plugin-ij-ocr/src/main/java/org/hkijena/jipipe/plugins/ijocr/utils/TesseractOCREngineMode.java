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

package org.hkijena.jipipe.plugins.ijocr.utils;

public enum TesseractOCREngineMode {
    OEM0(0, "Legacy engine only"),
    OEM1(1, "Neural nets LSTM engine only"),
    OEM2(2, "Legacy + LSTM engines"),
    OEM3(3, "Automatic (Default)");

    private final int nativeValue;
    private final String label;

    TesseractOCREngineMode(int nativeValue, String label) {
        this.nativeValue = nativeValue;
        this.label = label;
    }

    public int getNativeValue() {
        return nativeValue;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label + " (OEM " + nativeValue + ")";
    }
}
