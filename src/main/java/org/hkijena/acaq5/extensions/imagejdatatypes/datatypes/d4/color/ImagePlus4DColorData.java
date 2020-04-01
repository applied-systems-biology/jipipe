package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;

/**
 * Color 4D image
 */
@ACAQDocumentation(name = "4D image (color)")
@ACAQOrganization(menuPath = "Images\n4D\nColor")
public class ImagePlus4DColorData extends ImagePlus4DData {
    /**
     * @param image wrapped image
     */
    public ImagePlus4DColorData(ImagePlus image) {
        super(image);
    }
}
