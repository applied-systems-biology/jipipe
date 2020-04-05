package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;

/**
 * Color 4D image
 */
@ACAQDocumentation(name = "4D image (color)")
@ACAQOrganization(menuPath = "Images\n4D\nColor")
public class ImagePlus4DColorData extends ImagePlus4DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DColorData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
            System.out.println("[WARNING] Attempt to store non-color data into a color image. Converting to RGB.");
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
        }
    }
}
