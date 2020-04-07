package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 16-bit greyscale 4D image
 */
@ACAQDocumentation(name = "4D image (16 bit)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscale16UData extends ImagePlus4DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscale16UData(ImagePlus image) {
        super(image);

        // Apply conversion
        if (image.getType() != ImagePlus.GRAY16) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray16();
        }
    }
}
