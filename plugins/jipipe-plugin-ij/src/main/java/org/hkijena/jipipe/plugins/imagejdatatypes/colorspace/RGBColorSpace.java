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

package org.hkijena.jipipe.plugins.imagejdatatypes.colorspace;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class RGBColorSpace implements ColorSpace {

    public static final RGBColorSpace INSTANCE = new RGBColorSpace();

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

    @Override
    public int getNChannels() {
        return 3;
    }

    @Override
    public String getChannelName(int channelIndex) {
        switch (channelIndex) {
            case 0:
                return "Red";
            case 1:
                return "Green";
            case 2:
                return "Blue";
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String getChannelShortName(int channelIndex) {
        switch (channelIndex) {
            case 0:
                return "R";
            case 1:
                return "G";
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
