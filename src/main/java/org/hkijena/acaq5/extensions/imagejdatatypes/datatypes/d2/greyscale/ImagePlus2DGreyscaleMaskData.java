package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "2D image (mask)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscaleMaskData extends ImagePlus2DGreyscale8UData {
    public ImagePlus2DGreyscaleMaskData(ImagePlus image) {
        super(image);
    }
}
