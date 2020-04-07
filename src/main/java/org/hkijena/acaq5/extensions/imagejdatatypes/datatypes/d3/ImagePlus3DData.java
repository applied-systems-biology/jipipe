package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * 3D image
 */
@ACAQDocumentation(name = "3D image")
@ACAQOrganization(menuPath = "Images\n3D")
public class ImagePlus3DData extends ImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DData(ImagePlus image) {
        super(image);

        if (image.getNDimensions() > 3) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 3D data!");
        }
    }
}
