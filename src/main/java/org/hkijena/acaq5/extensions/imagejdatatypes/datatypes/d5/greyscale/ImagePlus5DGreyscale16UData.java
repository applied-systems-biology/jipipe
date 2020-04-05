package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 16-bit greyscale 5D image
 */
@ACAQDocumentation(name = "5D image (16 bit)")
@ACAQOrganization(menuPath = "Images\n5D\nGreyscale")
public class ImagePlus5DGreyscale16UData extends ImagePlus5DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * @param image wrapped image
     */
    public ImagePlus5DGreyscale16UData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.GRAY16) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray16();
        }
    }
}
