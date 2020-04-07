package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 32 bit float greyscale 2D image
 */
@ACAQDocumentation(name = "2D image (float)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscale32FData extends ImagePlus2DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DGreyscale32FData(ImagePlus image) {
        super(image);

        // Apply conversion
        if (image.getType() != ImagePlus.GRAY32) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray32();
        }
    }
}
