package org.hkijena.jipipe.extensions.imagejdatatypes.colorspace;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

/**
 * A {@link ColorSpace} that indicates that no color space information is set.
 * To be used in {@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImageTypeInfo}
 */
public class GreyscaleColorSpace implements ColorSpace {

    public static final GreyscaleColorSpace INSTANCE = new GreyscaleColorSpace();

    @Override
    public void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo) {
        ImageJUtils.convertToColorRGBIfNeeded(img);
    }

    @Override
    public void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo) {
        if (imgSpace.getClass() != GreyscaleColorSpace.class) {
            if (img.getType() == ImagePlus.COLOR_RGB) {
                // Make greyscale
                ImageJUtils.convertToGreyscaleIfNeeded(img);
            }
        }
    }

    @Override
    public int convert(int pixel, ColorSpace imgSpace) {
        if (imgSpace.getClass() != GreyscaleColorSpace.class) {
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
        return "Greyscale";
    }

    @Override
    public int getNChannels() {
        return 1;
    }

    @Override
    public String getChannelName(int channelIndex) {
        return "Value";
    }

    @Override
    public String getChannelShortName(int channelIndex) {
        return "v";
    }

    @Override
    public int composePixel(int... channelValues) {
        return channelValues[0];
    }

    @Override
    public void decomposePixel(int pixel, int[] channelValues) {
        channelValues[0] = pixel;
    }
}
