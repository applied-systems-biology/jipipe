package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit color 4D image
 */
@ACAQDocumentation(name = "4D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n4D\nColor")
public class ImagePlus4DColor8UData extends ImagePlus4DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DColor8UData(ImagePlus image) {
        super(image);

        // Apply conversion
        if (image.getType() != ImagePlus.COLOR_256) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
            ic.convertRGBtoIndexedColor(256);
        }
    }
}
