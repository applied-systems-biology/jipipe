package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "4D image (RGB)")
@ACAQOrganization(menuPath = "Images\n4D\nColor")
public class ImagePlus4DColorRGBData extends ImagePlus4DColorData {
    public ImagePlus4DColorRGBData(ImagePlus image) {
        super(image);
    }
}
