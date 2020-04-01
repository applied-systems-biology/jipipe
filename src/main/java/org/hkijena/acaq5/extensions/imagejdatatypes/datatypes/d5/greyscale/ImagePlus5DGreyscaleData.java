package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;

/**
 * Greyscale 5D image
 */
@ACAQDocumentation(name = "5D image (greyscale)")
@ACAQOrganization(menuPath = "Images\n5D\nGreyscale")
public class ImagePlus5DGreyscaleData extends ImagePlus5DData {
    /**
     * @param image wrapped image
     */
    public ImagePlus5DGreyscaleData(ImagePlus image) {
        super(image);
    }
}
