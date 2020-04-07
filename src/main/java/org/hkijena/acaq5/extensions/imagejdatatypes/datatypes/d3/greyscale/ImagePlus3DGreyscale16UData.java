package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 16-bit greyscale 3D image
 */
@ACAQDocumentation(name = "3D image (16 bit)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscale16UData extends ImagePlus3DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DGreyscale16UData(ImagePlus image) {
        super(image);

        // Apply conversion
        if (image.getType() != ImagePlus.GRAY16) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray16();
        }
    }
}
