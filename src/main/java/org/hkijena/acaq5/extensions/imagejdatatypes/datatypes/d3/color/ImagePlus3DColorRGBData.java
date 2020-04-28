package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;

/**
 * RGB color 3D image
 */
@ACAQDocumentation(name = "3D image (RGB)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColorRGBData extends ImagePlus3DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColorRGBData(ImagePlus image) {
        super(ImagePlusColorRGBData.convertIfNeeded(image));
    }
}
