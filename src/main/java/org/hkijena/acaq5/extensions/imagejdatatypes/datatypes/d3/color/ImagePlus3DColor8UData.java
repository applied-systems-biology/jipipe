package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit color 3D image
 */
@ACAQDocumentation(name = "3D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColor8UData extends ImagePlus3DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColor8UData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.COLOR_256) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
            ic.convertRGBtoIndexedColor(256);
        }
    }
}
