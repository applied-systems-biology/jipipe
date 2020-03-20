package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

@ACAQDocumentation(name = "2D image")
@ACAQOrganization(menuPath = "Images\n2D")
public class ImagePlus2DData extends ImagePlusData {
    public ImagePlus2DData(ImagePlus image) {
        super(image);
    }
}
