package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;

/**
 * Greyscale 2D image
 */
@ACAQDocumentation(name = "2D image (greyscale)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscaleData extends ImagePlus2DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DGreyscaleData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.GRAY8 &&
                image.getType() != ImagePlus.GRAY16 &&
                image.getType() != ImagePlus.GRAY32) {
            System.out.println("[WARNING] Attempt to store non-grayscale data into a grayscale image. Converting to 32-bit floating point.");
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray32();
        }
    }
}
