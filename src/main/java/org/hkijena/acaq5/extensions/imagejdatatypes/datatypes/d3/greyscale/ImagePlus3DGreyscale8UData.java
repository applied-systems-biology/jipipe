package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit greyscale 3D image
 */
@ACAQDocumentation(name = "3D image (8 bit)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscale8UData extends ImagePlus3DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus3DGreyscale8UData(ImagePlus image) {
        super(image);
    }
}
