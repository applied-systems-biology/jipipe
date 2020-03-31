package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

@ACAQDocumentation(name = "3D image")
@ACAQOrganization(menuPath = "Images\n3D")
public class ImagePlus3DData extends ImagePlusData {
    public ImagePlus3DData(ImagePlus image) {
        super(image);
    }
}
