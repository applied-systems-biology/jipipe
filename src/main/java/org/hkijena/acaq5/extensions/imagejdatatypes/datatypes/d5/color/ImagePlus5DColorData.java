package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.ImagePlus5DData;

@ACAQDocumentation(name = "5D image (color)")
@ACAQOrganization(menuPath = "Images\n5D\nColor")
public class ImagePlus5DColorData extends ImagePlus5DData {
    public ImagePlus5DColorData(ImagePlus image) {
        super(image);
    }
}
