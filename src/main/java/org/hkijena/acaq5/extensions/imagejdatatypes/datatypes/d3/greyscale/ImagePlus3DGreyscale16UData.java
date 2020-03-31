package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

@ACAQDocumentation(name = "3D image (16 bit)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscale16UData extends ImagePlus3DGreyscaleData {
    public ImagePlus3DGreyscale16UData(ImagePlus image) {
        super(image);
    }
}
