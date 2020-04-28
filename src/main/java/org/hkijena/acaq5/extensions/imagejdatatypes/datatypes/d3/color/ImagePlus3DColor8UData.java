package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;

/**
 * 8-bit color 3D image
 */
@ACAQDocumentation(name = "3D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColor8UData extends ImagePlus3DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColor8UData(ImagePlus image) {
        super(ImagePlusColor8UData.convertIfNeeded(image));
    }
}
