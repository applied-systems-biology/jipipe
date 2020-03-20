package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;

@ACAQDocumentation(name = "2D image (greyscale)")
@ACAQOrganization(menuPath = "Images\n2D\nGreyscale")
public class ImagePlus2DGreyscaleData extends ImagePlus2DData {
    public ImagePlus2DGreyscaleData(ImagePlus image) {
        super(image);
    }
}
