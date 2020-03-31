package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.ImagePlus4DData;

@ACAQDocumentation(name = "4D image (greyscale)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscaleData extends ImagePlus4DData {
    public ImagePlus4DGreyscaleData(ImagePlus image) {
        super(image);
    }
}
