package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d4;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * 4D image
 */
@ACAQDocumentation(name = "4D image")
@ACAQOrganization(menuPath = "Images\n4D")
public class ImagePlus4DData extends ImagePlusData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DData(ImagePlus image) {
        super(image);

        if(image.getNDimensions() > 4) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 4D data!");
        }
    }
}
