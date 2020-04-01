package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit color 5D image
 */
@ACAQDocumentation(name = "5D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n5D\nColor")
public class ImagePlus5DColor8UData extends ImagePlus5DColorData {
    /**
     * @param image wrapped image
     */
    public ImagePlus5DColor8UData(ImagePlus image) {
        super(image);
    }
}
