package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.greyscale;

import ij.ImagePlus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;

/**
 * 8-bit mask without dimension.
 * These image data types exist to address general processing solely based on bit-depth (e.g. process all 2D image planes).
 * Conversion works through {@link org.hkijena.acaq5.extensions.imagejdatatypes.algorithms.ImplicitImageTypeConverter}
 */
@ACAQDocumentation(name = "Image (mask)")
@ACAQOrganization(menuPath = "Images\nGreyscale")
public class ImagePlusGreyscaleMaskData extends ImagePlusGreyscale8UData {

    /**
     * The dimensionality of this data.
     * -1 means that we do not have information about the dimensionality
     */
    public static final int DIMENSIONALITY = -1;

    /**
     * @param image wrapped image
     */
    public ImagePlusGreyscaleMaskData(ImagePlus image) {
        super(ImagePlusGreyscale8UData.convertIfNeeded(image));
    }
}
