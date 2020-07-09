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

package org.hkijena.pipelinej.extensions.imagejdatatypes.datatypes.greyscale;

import ij.ImagePlus;
import ij.process.ImageConverter;
import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.ACAQOrganization;

/**
 * 32 bit float greyscale image without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.pipelinej.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@ACAQDocumentation(name = "Image (float)")
@ACAQOrganization(menuPath = "Images\nGreyscale")
public class ImagePlusGreyscale32FData extends ImagePlusGreyscaleData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * @param image wrapped image
     */
    public ImagePlusGreyscale32FData(ImagePlus image) {
        super(ImagePlusGreyscale32FData.convertIfNeeded(image));
    }

    /**
     * Converts an {@link ImagePlus} to the color space of this data.
     * Does not guarantee that the input image is copied.
     *
     * @param image the image
     * @return converted image.
     */
    public static ImagePlus convertIfNeeded(ImagePlus image) {
        if (image.getType() != ImagePlus.GRAY32) {
            ImageConverter ic = new ImageConverter(image);
            ic.convertToGray32();
        }
        return image;
    }
}
