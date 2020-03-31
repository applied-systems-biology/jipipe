package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "3D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColor8UData extends ImagePlus3DColorData {
    public ImagePlus3DColor8UData(ImagePlus image) {
        super(image);
    }
}
