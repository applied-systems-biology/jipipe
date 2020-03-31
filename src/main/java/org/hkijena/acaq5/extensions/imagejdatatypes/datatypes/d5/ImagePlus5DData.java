package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

@ACAQDocumentation(name = "5D image")
@ACAQOrganization(menuPath = "Images\n5D")
public class ImagePlus5DData extends ImagePlusData {
    public ImagePlus5DData(ImagePlus image) {
        super(image);
    }
}
