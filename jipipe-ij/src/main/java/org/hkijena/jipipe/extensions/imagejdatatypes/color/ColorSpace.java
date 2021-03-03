package org.hkijena.jipipe.extensions.imagejdatatypes.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A class that governs conversion between color spaces
 */
public interface ColorSpace {
    /**
     * Converts the color image into RGB (in-place)
     * @param img the image
     * @param progressInfo  the progress info
     */
    void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo);

    /**
     * Converts the image into the current color space
     * @param img the image
     * @param imgSpace the color space of the image
     * @param progressInfo the progress
     */
    void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo);
}
