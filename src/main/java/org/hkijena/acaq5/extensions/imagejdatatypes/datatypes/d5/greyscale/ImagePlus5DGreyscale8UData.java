package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit greyscale 5D image
 */
@ACAQDocumentation(name = "5D image (8 bit)")
@ACAQOrganization(menuPath = "Images\n5D\nGreyscale")
public class ImagePlus5DGreyscale8UData extends ImagePlus5DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus5DGreyscale8UData(ImagePlus image) {
        super(image);
    }
}
