package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d2.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

/**
 * RGB colored 2D image
 */
@ACAQDocumentation(name = "2D image (RGB)")
@ACAQOrganization(menuPath = "Images\n2D\nColor")
public class ImagePlus2DColorRGBData extends ImagePlus2DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DColorRGBData(ImagePlus image) {
        super(ImagePlusColorRGBData.convertIfNeeded(image));
    }
}
