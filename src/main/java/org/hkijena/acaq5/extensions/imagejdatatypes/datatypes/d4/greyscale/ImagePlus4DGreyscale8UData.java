package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit greyscale 4D image
 */
@ACAQDocumentation(name = "4D image (8 bit)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscale8UData extends ImagePlus4DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscale8UData(ImagePlus image) {
        super(image);
    }
}
