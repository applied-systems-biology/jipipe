package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * 5D image
 */
@ACAQDocumentation(name = "5D image")
@ACAQOrganization(menuPath = "Images\n5D")
public class ImagePlus5DData extends ImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * @param image wrapped image
     */
    public ImagePlus5DData(ImagePlus image) {
        super(image);

        if(image.getNDimensions() > 5) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 5D data!");
        }
    }
}
