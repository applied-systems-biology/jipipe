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

package org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.d4.greyscale;

import ij.ImagePlus;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;

/**
 * Mask 4D image
 */
@ACAQDocumentation(name = "4D image (mask)")
@ACAQOrganization(menuPath = "Images\n4D\nGreyscale")
public class ImagePlus4DGreyscaleMaskData extends ImagePlus4DGreyscale8UData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 4;

    /**
     * @param image wrapped image
     */
    public ImagePlus4DGreyscaleMaskData(ImagePlus image) {
        super(ImagePlusGreyscale8UData.convertIfNeeded(image));
    }
}
