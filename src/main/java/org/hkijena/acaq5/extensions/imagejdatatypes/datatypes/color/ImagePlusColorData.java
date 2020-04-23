package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * Colored image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.acaq5.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@ACAQDocumentation(name = "Image (color)")
@ACAQOrganization(menuPath = "Images\nColor")
public class ImagePlusColorData extends ImagePlusData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * @param image wrapped image
     */
    public ImagePlusColorData(ImagePlus image) {
        super(image);

        // Apply conversion
        if (image.getType() != ImagePlus.COLOR_256 && image.getType() != ImagePlus.COLOR_RGB) {
            System.out.println("[WARNING] Attempt to store non-color data into a color image. Converting to RGB.");
            ImageConverter ic = new ImageConverter(image);
            ic.convertToRGB();
        }
    }
}
