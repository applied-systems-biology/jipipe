package org.hkijena.jipipe.extensions.imagejdatatypes.colorspace;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A {@link ColorSpace} that indicates that no color space information is set.
 * To be used in {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo}
 */
public class IgnoreColorSpace implements ColorSpace {
    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        // Do nothing
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
       // Do nothing
    }

    @Override
    public int convert(int pixel, ColorSpace imgSpace) {
        if (imgSpace.getClass() != IgnoreColorSpace.class) {
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
        return "No color space";
    }

    @Override
    public int getNChannels() {
        return 0;
    }

    @Override
    public String getChannelName(int channelIndex) {
        throw new IllegalArgumentException();
    }

    @Override
    public String getChannelShortName(int channelIndex) {
        throw new IllegalArgumentException();
    }

    @Override
    public int composePixel(int... channelValues) {
        throw new IllegalArgumentException();
    }

    @Override
    public void decomposePixel(int pixel, int[] channelValues) {
        throw new IllegalArgumentException();
    }
}
