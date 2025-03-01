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

public enum TesseractPageSegmentationMethod {
    PSM0(0, "Orientation and script detection (OSD) only"),
    PSM1(1, "Automatic page segmentation with OSD"),
    PSM2(2, "Automatic page segmentation, but no OSD, or OCR"),
    PSM3(3, "Fully automatic page segmentation, but no OSD"),
    PSM4(4, "Assume a single column of text of variable sizes"),
    PSM5(5, " Assume a single uniform block of vertically aligned text"),
    PSM6(6, "Assume a single uniform block of text"),
    PSM7(7, "Treat the image as a single text line"),
    PSM8(8, "Treat the image as a single word"),
    PSM9(9, "Treat the image as a single word in a circle"),
    PSM10(10, "Treat the image as a single character"),
    PSM11(11, "Sparse text (find as much text as possible)"),
    PSM12(12, "Sparse text with OSD"),
    PSM13(13, "Treat the image as a single text line (disable Tesseract-specific hacks)");

    private final int nativeValue;
    private final String label;

    TesseractPageSegmentationMethod(int nativeValue, String label) {
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
        return label + " (PSM " + nativeValue + ")";
    }
}
