package org.hkijena.jipipe.extensions.imagejdatatypes.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class RGBColorSpace implements ColorSpace {
    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        // Do nothing
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
        if(imgSpace.getClass() != RGBColorSpace.class) {
            // Convert to RGB
            imgSpace.convertToRGB(img, progressInfo);
        }
    }
}
