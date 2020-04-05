package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;

/**
 * Colored 2D image
 */
@ACAQDocumentation(name = "2D image (color)")
@ACAQOrganization(menuPath = "Images\n2D\nColor")
public class ImagePlus2DColorData extends ImagePlus2DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DColorData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
            System.out.println("[WARNING] Attempt to store non-color data into a color image. Converting to RGB.");
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
        }
    }
}
