package org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace;

/**
 * {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData} that has a color space attached to it.
 * Should contain a static {@link org.hkijena.jipipe.extensions.imagejdatatypes.color.ColorSpace} instance COLOR_SPACE
 */
public interface ColoredImagePlusData extends JIPipeData {
    ColorSpace getColorSpace();
}
