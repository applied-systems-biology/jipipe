package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

@ACAQDocumentation(name = "3D image (color)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColorData extends ImagePlus3DData {
    public ImagePlus3DColorData(ImagePlus image) {
        super(image);
    }
}
