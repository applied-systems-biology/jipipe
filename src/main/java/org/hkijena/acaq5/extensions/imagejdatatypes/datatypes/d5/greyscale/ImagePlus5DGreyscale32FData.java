package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "5D image (float)")
@ACAQOrganization(menuPath = "Images\n5D\nGreyscale")
public class ImagePlus5DGreyscale32FData extends ImagePlus5DGreyscaleData {
    public ImagePlus5DGreyscale32FData(ImagePlus image) {
        super(image);
    }
}
