package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "5D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n5D\nColor")
public class ImagePlus5DColor8UData extends ImagePlus5DColorData {
    public ImagePlus5DColor8UData(ImagePlus image) {
        super(image);
    }
}
