package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;

/**
 * Greyscale 4D image
 */
@ACAQDocumentation(name = "4D image (greyscale)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscaleData extends ImagePlus4DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscaleData(ImagePlus image) {
        super(image);

        // Apply conversion
        if (image.getType() != ImagePlus.GRAY8 &&
                image.getType() != ImagePlus.GRAY16 &&
                image.getType() != ImagePlus.GRAY32) {
            System.out.println("[WARNING] Attempt to store non-grayscale data into a grayscale image. Converting to 32-bit floating point.");
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray32();
        }
    }
}
