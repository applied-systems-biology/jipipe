/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale8UData;

import java.nio.file.Path;

/**
 * 3D image
 */
@JIPipeDocumentation(name = "3D image")
@JIPipeOrganization(menuPath = "Images\n3D")
@JIPipeHeavyData
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
            throw new UserFriendlyRuntimeException(new IllegalArgumentException("Trying to fit higher-dimensional data into " + DIMENSIONALITY + "D data!"),
                    "Trying to fit higher-dimensional data into " + DIMENSIONALITY + "D data!",
                    "ImageJ integration internals",
                    image.getNDimensions() + "D data was supplied, but it was requested that it should fit into " + DIMENSIONALITY + "D data. " +
                            "This is not trivial. This can be caused by selecting the wrong data slot type or applying a conversion" +
                            " from N-dimensional data into data with a defined dimensionality.",
                    "Try to check if the data slots have the correct data types. You can also check the input of the offending algorithm via " +
                            "the quick run to see if they fit the assumptions. If you cannot find the reason behind this error," +
                            " try to contact the JIPipe or plugin developers.");
        }
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus3DData(ImagePlusData.importImagePlusFrom(storageFolder));
    }
}
