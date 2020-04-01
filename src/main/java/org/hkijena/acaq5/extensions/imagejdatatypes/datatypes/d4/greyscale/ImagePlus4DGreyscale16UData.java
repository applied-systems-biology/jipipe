package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 16-bit greyscale 4D image
 */
@ACAQDocumentation(name = "4D image (16 bit)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscale16UData extends ImagePlus4DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscale16UData(ImagePlus image) {
        super(image);
    }
}
