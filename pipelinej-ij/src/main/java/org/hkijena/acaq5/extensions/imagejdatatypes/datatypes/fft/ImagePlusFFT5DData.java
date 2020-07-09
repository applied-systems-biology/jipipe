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

package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.fft;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 5D image in frequency space
 */
@ACAQDocumentation(name = "5D FFT Image")
@ACAQOrganization(menuPath = "Images\nFFT")
public class ImagePlusFFT5DData extends ImagePlusFFT2DData {

    /**
     * The dimensionality of this data
     */
    public static final int DIMENSIONALITY = 5;

    /**
     * Creates a new instance
     *
     * @param image wrapped image
     */
    public ImagePlusFFT5DData(ImagePlus image) {
        super(image);

        if (image.getNDimensions() > 5) {
            throw new IllegalArgumentException("Trying to fit higher-dimensional data into 5D data!");
        }
    }

}
