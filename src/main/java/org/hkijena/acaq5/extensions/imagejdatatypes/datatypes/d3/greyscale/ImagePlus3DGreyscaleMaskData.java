package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit mask 3D image
 */
@ACAQDocumentation(name = "3D image (mask)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscaleMaskData extends ImagePlus3DGreyscale8UData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DGreyscaleMaskData(ImagePlus image) {
        super(image);

        // Apply conversion
        if(image.getType() != ImagePlus.GRAY8) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray8();
        }
    }
}
