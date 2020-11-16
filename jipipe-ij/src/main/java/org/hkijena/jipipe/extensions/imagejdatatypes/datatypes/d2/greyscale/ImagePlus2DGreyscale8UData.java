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

package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.greyscale;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;

import java.nio.file.Path;

/**
 * 8-bit greyscale 2D image
 */
@JIPipeDocumentation(name = "2D image (8 bit)")
@JIPipeOrganization(menuPath = "Images\n2D\nGreyscale")
@JIPipeHeavyData
public class ImagePlus2DGreyscale8UData extends ImagePlus2DGreyscaleData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 2;

    /**
     * @param image wrapped image
     */
    public ImagePlus2DGreyscale8UData(ImagePlus image) {
        super(ImagePlusGreyscale8UData.convertIfNeeded(image));
    }

    public static ImagePlusData importFrom(Path storageFolder) {
        return new ImagePlus2DGreyscale8UData(ImagePlusData.importImagePlusFrom(storageFolder));
    }
}
