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
        if (imgSpace.getClass() != RGBColorSpace.class) {
            imgSpace.convertToRGB(img, progressInfo);
        }
    }

    @Override
    public int convert(int pixel, ColorSpace imgSpace) {
        if (imgSpace.getClass() != RGBColorSpace.class) {
            return imgSpace.convertToRGB(pixel);
        } else {
            return pixel;
        }
    }

    @Override
    public int convertToRGB(int pixel) {
        return pixel;
    }

    @Override
    public String toString() {
        return "RGB";
    }
}
