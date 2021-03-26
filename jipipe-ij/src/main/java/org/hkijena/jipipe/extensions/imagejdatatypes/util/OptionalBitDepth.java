package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterSettings;

/**
 * Enum of ImageJ bit depths
 */
@EnumParameterSettings(itemInfo = OptionalBitDepthEnumItemInfo.class)
public enum OptionalBitDepth {
    None("", 0),
    Grayscale8u("8-bit", 8),
    Grayscale16u("16-bit", 16),
    Grayscale32f("32-bit", 32),
    ColorRGB("RGB", 24);

    private final String typeName;
    private final int bitDepth;

    OptionalBitDepth(String typeName, int bitDepth) {

        this.typeName = typeName;
        this.bitDepth = bitDepth;
    }

    @Override
    public String toString() {
        switch (this) {
            case None:
                return "None selected";
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
