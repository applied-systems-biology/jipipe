package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit mask 3D image
 */
@ACAQDocumentation(name = "3D image (mask)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscaleMaskData extends ImagePlus3DGreyscale8UData {
    /**
     * @param image wrapped image
     */
    public ImagePlus3DGreyscaleMaskData(ImagePlus image) {
        super(image);
    }
}
