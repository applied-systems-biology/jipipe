package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "2D image (RGB)")
@ACAQOrganization(menuPath = "Images\n2D\nColor")
public class ImagePlus2DColorRGBData extends ImagePlus2DColorData {
    public ImagePlus2DColorRGBData(ImagePlus image) {
        super(image);
    }
}
