package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * Mask 4D image
 */
@ACAQDocumentation(name = "4D image (mask)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscaleMaskData extends ImagePlus4DGreyscale8UData {
    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscaleMaskData(ImagePlus image) {
        super(image);
    }
}
