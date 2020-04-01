package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 32-bit floating point greyscale 3D image
 */
@ACAQDocumentation(name = "3D image (float)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscale32FData extends ImagePlus3DGreyscaleData {
    /**
     * @param image wrapped image
     */
    public ImagePlus3DGreyscale32FData(ImagePlus image) {
        super(image);
    }
}
