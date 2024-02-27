/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.colorspace;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

import java.awt.*;

public class HSBColorSpace implements ColorSpace {

    public static final HSBColorSpace INSTANCE = new HSBColorSpace();

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

    @Override
    public int getNChannels() {
        return 3;
    }

    @Override
    public String getChannelName(int channelIndex) {
        switch (channelIndex) {
            case 0:
                return "Hue";
            case 1:
                return "Saturation";
            case 2:
                return "Brightness";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String getChannelShortName(int channelIndex) {
        switch (channelIndex) {
            case 0:
                return "H";
            case 1:
                return "S";
            case 2:
                return "B";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int composePixel(int... channelValues) {
        return (channelValues[0] << 16) + (channelValues[1] << 8) + channelValues[2];
    }

    @Override
    public void decomposePixel(int pixel, int[] channelValues) {
        int x1 = (pixel & 0xff0000) >> 16;
        int x2 = (pixel & 0xff00) >> 8;
        int x3 = pixel & 0xff;

        channelValues[0] = x1;
        channelValues[1] = x2;
        channelValues[2] = x3;
    }
}
