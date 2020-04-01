package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit color 4D image
 */
@ACAQDocumentation(name = "4D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n4D\nColor")
public class ImagePlus4DColor8UData extends ImagePlus4DColorData {
    /**
     * @param image wrapped image
     */
    public ImagePlus4DColor8UData(ImagePlus image) {
        super(image);
    }
}
