package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

@ACAQDocumentation(name = "3D image (greyscale)")
@ACAQOrganization(menuPath = "Images\n3D\nGreyscale")
public class ImagePlus3DGreyscaleData extends ImagePlus3DData {
    public ImagePlus3DGreyscaleData(ImagePlus image) {
        super(image);
    }
}
