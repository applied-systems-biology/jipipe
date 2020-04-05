package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;

/**
 * Color 5D image
 */
@ACAQDocumentation(name = "5D image (color)")
@ACAQOrganization(menuPath = "Images\n5D\nColor")
public class ImagePlus5DColorData extends ImagePlus5DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
            System.out.println("[WARNING] Attempt to store non-color data into a color image. Converting to RGB.");
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
        }
    }
}
