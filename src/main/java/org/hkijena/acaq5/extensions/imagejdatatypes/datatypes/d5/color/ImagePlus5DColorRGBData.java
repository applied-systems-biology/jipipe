package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * RGB color 5D image
 */
@ACAQDocumentation(name = "5D image (RGB)")
@ACAQOrganization(menuPath = "Images\n5D\nColor")
public class ImagePlus5DColorRGBData extends ImagePlus5DColorData {
    /**
     * @param image wrapped image
     */
    public ImagePlus5DColorRGBData(ImagePlus image) {
        super(image);
    }
}
