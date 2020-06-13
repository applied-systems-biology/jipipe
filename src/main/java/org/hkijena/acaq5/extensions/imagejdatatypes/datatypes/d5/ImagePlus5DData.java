package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
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

        if (image.getNDimensions() > 5) {
            throw new UserFriendlyRuntimeException(new IllegalArgumentException("Trying to fit higher-dimensional data into " + DIMENSIONALITY + "D data!"),
                    "Trying to fit higher-dimensional data into " + DIMENSIONALITY + "D data!",
                    "ImageJ integration internals",
                    image.getNDimensions() + "D data was supplied, but it was requested that it should fit into " + DIMENSIONALITY + "D data. " +
                            "This is not trivial. This can be caused by selecting the wrong data slot type or applying a conversion" +
                            " from N-dimensional data into data with a defined dimensionality.",
                    "Try to check if the data slots have the correct data types. You can also check the input of the offending algorithm via " +
                            "the quick run to see if they fit the assumptions. If you cannot find the reason behind this error," +
                            " try to contact the ACAQ5 or plugin developers.");
        }
    }
}
