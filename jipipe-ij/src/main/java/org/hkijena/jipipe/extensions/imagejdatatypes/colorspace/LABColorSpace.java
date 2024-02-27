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
import ij.process.ColorSpaceConverter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

public class LABColorSpace implements ColorSpace {

    public static final LABColorSpace INSTANCE = new LABColorSpace();
    private static final ColorSpaceConverter CONVERTER = new ColorSpaceConverter();

    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJUtils.convertLABToRGB(img, progressInfo);
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
        if (imgSpace.getClass() != LABColorSpace.class) {
            imgSpace.convertToRGB(img, progressInfo);
            ImageJUtils.convertRGBToLAB(img, progressInfo);
        }
    }

    @Override
    public int convert(int pixel, ColorSpace imgSpace) {
        if (imgSpace.getClass() != LABColorSpace.class) {
            pixel = imgSpace.convertToRGB(pixel);
            double[] lab = CONVERTER.RGBtoLAB(pixel);
            int l = (int) Math.max(0, Math.min(255, lab[0]));
            int a = (int) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[1])) - (int) Byte.MIN_VALUE;
            int b = (int) Math.max(Byte.MIN_VALUE, Math.min(Byte.MAX_VALUE, lab[2])) - (int) Byte.MIN_VALUE;
            pixel = (l << 16) + (a << 8) + b;
        }
        return pixel;
    }

    @Override
    public int convertToRGB(int pixel) {
        double[] lab = new double[3];
        int l = (pixel & 0xff0000) >> 16;
        int a = ((pixel & 0xff00) >> 8) + Byte.MIN_VALUE;
        int b = (pixel & 0xff) + Byte.MIN_VALUE;
        lab[0] = l;
        lab[1] = a;
        lab[2] = b;
        int[] rgb = CONVERTER.LABtoRGB(lab);
        return (rgb[0] << 16) + (rgb[1] << 8) + rgb[2];
    }

    @Override
    public int getNChannels() {
        return 3;
    }

    @Override
    public String getChannelName(int channelIndex) {
        switch (channelIndex) {
            case 0:
                return "L*";
            case 1:
                return "a*";
            case 2:
                return "b*";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String getChannelShortName(int channelIndex) {
        switch (channelIndex) {
            case 0:
                return "L*";
            case 1:
                return "a*";
            case 2:
                return "b*";
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

    @Override
    public String toString() {
        return "L*a*b*";
    }
}
