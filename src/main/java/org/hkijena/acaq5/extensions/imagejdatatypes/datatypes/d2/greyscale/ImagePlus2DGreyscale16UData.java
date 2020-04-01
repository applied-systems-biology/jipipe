package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 16-bit greyscale 2D image
 */
@ACAQDocumentation(name = "2D image (16 bit)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscale16UData extends ImagePlus2DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus2DGreyscale16UData(ImagePlus image) {
        super(image);
    }
}
