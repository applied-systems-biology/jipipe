package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "5D image (mask)")
@ACAQOrganization(menuPath = "Images\n5D\nGreyscale")
public class ImagePlus5DGreyscaleMaskData extends ImagePlus5DGreyscale8UData {
    public ImagePlus5DGreyscaleMaskData(ImagePlus image) {
        super(image);
    }
}
