package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

/**
 * Color 3D image
 */
@ACAQDocumentation(name = "3D image (color)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColorData extends ImagePlus3DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColorData(ImagePlus image) {
        super(ImagePlusColorData.convertIfNeeded(image));
    }
}
