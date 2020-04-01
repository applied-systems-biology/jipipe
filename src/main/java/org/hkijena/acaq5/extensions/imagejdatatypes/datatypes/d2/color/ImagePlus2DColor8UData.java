package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit color 2D image
 */
@ACAQDocumentation(name = "2D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n2D\nColor")
public class ImagePlus2DColor8UData extends ImagePlus2DColorData {
    /**
     * @param image wrapped image
     */
    public ImagePlus2DColor8UData(ImagePlus image) {
        super(image);
    }
}
