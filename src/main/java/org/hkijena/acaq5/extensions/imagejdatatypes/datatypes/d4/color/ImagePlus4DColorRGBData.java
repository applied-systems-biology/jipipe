package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * RGB color 4D image
 */
@ACAQDocumentation(name = "4D image (RGB)")
@ACAQOrganization(menuPath = "Images\n4D\nColor")
public class ImagePlus4DColorRGBData extends ImagePlus4DColorData {
    /**
     * @param image wrapped image
     */
    public ImagePlus4DColorRGBData(ImagePlus image) {
        super(image);
    }
}
