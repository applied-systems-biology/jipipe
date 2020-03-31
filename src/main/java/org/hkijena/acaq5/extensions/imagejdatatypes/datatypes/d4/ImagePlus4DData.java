package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

@ACAQDocumentation(name = "4D image")
@ACAQOrganization(menuPath = "Images\n4D")
public class ImagePlus4DData extends ImagePlusData {
    public ImagePlus4DData(ImagePlus image) {
        super(image);
    }
}
