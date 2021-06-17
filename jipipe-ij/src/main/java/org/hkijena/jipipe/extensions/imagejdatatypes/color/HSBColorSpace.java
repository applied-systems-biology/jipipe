package org.hkijena.jipipe.extensions.imagejdatatypes.color;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.Color;

public class HSBColorSpace implements ColorSpace {
    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJUtils.convertHSBToRGB(img, progressInfo);
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
        if (imgSpace.getClass() != HSBColorSpace.class) {
            imgSpace.convertToRGB(img, progressInfo);
            ImageJUtils.convertRGBToHSB(img, progressInfo);
        }
    }

    @Override
    public int convert(int pixel, ColorSpace imgSpace) {
        if (imgSpace.getClass() != HSBColorSpace.class) {
            pixel = imgSpace.convertToRGB(pixel);
            int r = (pixel & 0xff0000) >> 16;
            int g = (pixel & 0xff00) >> 8;
            int b = pixel & 0xff;
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            int H = ((int) (hsb[0] * 255.0));
            int S = ((int) (hsb[1] * 255.0));
            int B = ((int) (hsb[2] * 255.0));
            pixel = (H << 16) + (S << 8) + B;
        }
        return pixel;
    }

    @Override
    public int convertToRGB(int pixel) {
        int H = (pixel & 0xff0000) >> 16;
        int S = (pixel & 0xff00) >> 8;
        int B = pixel & 0xff;
        return Color.HSBtoRGB(H / 255.0f, S / 255.0f, B / 255.0f);
    }

    @Override
    public String toString() {
        return "HSB";
    }
}
