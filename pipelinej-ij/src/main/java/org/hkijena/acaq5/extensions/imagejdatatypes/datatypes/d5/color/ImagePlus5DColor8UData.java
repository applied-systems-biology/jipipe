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

package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d5.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColor8UData;

/**
 * 8-bit color 5D image
 */
@ACAQDocumentation(name = "5D Image (8-bit color)")
@ACAQOrganization(menuPath = "Images\n5D\nColor")
public class ImagePlus5DColor8UData extends ImagePlus5DColorData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * @param image wrapped image
     */
    public ImagePlus5DColor8UData(ImagePlus image) {
        super(ImagePlusColor8UData.convertIfNeeded(image));
    }
}
