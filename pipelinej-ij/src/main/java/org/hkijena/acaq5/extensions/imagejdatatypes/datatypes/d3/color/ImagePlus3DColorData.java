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

package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.color;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.color.ImagePlusColorData;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;

/**
 * Color 3D image
 */
@ACAQDocumentation(name = "3D image (color)")
@ACAQOrganization(menuPath = "Images\n3D\nColor")
public class ImagePlus3DColorData extends ImagePlus3DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 3;

    /**
     * @param image wrapped image
     */
    public ImagePlus3DColorData(ImagePlus image) {
        super(ImagePlusColorData.convertIfNeeded(image));
    }
}
