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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale16UData;

import java.nio.file.Path;

/**
 * 16-bit greyscale 4D image
 */
@JIPipeDocumentation(name = "4D image (16 bit)")
@JIPipeOrganization(menuPath = "Images\n4D\nGreyscale")
@JIPipeHeavyData
public class ImagePlus4DGreyscale16UData extends ImagePlus4DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscale16UData(ImagePlus image) {
        super(ImagePlusGreyscale16UData.convertIfNeeded(image));
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus4DGreyscale16UData(ImagePlusData.importImagePlusFrom(storageFolder));
    }

    /**
     * Converts the incoming image data into the current format.
     * @param data the data
     * @return the converted data
     */
    public static ImagePlusData convertFrom(ImagePlusData data) {
        return new ImagePlus4DGreyscale16UData(data.getImage());
    }
}
