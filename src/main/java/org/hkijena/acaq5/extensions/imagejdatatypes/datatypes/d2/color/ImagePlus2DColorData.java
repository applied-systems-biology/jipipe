package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

@ACAQDocumentation(name = "2D image (color)")
@ACAQOrganization(menuPath = "Images\n2D\nColor")
public class ImagePlus2DColorData extends ImagePlusData {
    public ImagePlus2DColorData(ImagePlus image) {
        super(image);
    }
}
