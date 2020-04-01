package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 32 bit float greyscale 2D image
 */
@ACAQDocumentation(name = "2D image (float)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscale32FData extends ImagePlus2DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus2DGreyscale32FData(ImagePlus image) {
        super(image);
    }
}
