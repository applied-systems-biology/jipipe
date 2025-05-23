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

package org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions;

import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

/**
 * Enum of ImageJ bit depths
 */
@EnumParameterSettings(itemInfo = BitDepthEnumItemInfo.class)
public enum BitDepth {
    Grayscale8u("8-bit", 8),
    Grayscale16u("16-bit", 16),
    Grayscale32f("32-bit", 32),
    ColorRGB("RGB", 24);

    private final String typeName;
    private final int bitDepth;

    BitDepth(String typeName, int bitDepth) {

        this.typeName = typeName;
        this.bitDepth = bitDepth;
    }

    @Override
    public String toString() {
        switch (this) {
            case ColorRGB:
                return "24-bit Color (RGB)";
            case Grayscale8u:
                return "8-bit grayscale";
            case Grayscale16u:
                return "16-bit grayscale";
            case Grayscale32f:
                return "32-bit grayscale";
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * @return type name. works with IJ.create
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return bit depth. works with IJ.create
     */
    public int getBitDepth() {
        return bitDepth;
    }


}
