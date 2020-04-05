package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit greyscale 2D image
 */
@ACAQDocumentation(name = "2D image (8 bit)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscale8UData extends ImagePlus2DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DGreyscale8UData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.GRAY8) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray8();
        }
    }
}
