package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * RGB color 3D image
 */
@ACAQDocumentation(name = "3D image (RGB)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColorRGBData extends ImagePlus3DColorData {
    /**
     * @param image wrapped image
     */
    public ImagePlus3DColorRGBData(ImagePlus image) {
        super(image);
    }
}
