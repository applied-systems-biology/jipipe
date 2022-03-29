package org.hkijena.jipipe.extensions.imagejdatatypes.colorspace;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A class that governs conversion between color spaces
 */
public interface ColorSpace {
    /**
     * Converts the color image into RGB (in-place)
     *
     * @param img          the image
     * @param progressInfo the progress info
     */
    void convertToRGB(ImagePlus img, JIPipeProgressInfo progressInfo);

    /**
     * Converts the image into the current color space
     *
     * @param img          the image
     * @param imgSpace     the color space of the image
     * @param progressInfo the progress
     */
    void convert(ImagePlus img, ColorSpace imgSpace, JIPipeProgressInfo progressInfo);

    /**
     * Converts a single pixel from its source image space into this one
     *
     * @param pixel    the pixel (24 bit)
     * @param imgSpace the pixel image space
     * @return the converted pixel
     */
    int convert(int pixel, ColorSpace imgSpace);

    /**
     * Converts a single pixel from its source image space into RGB
     *
     * @param pixel the pixel (24 bit)
     * @return the converted pixel (in RGB)
     */
    int convertToRGB(int pixel);

    /**
     * Returns the number of pixel channels.
     * For example, 3 for RGB colors
     *
     * @return number of pixel channels
     */
    int getNChannels();

    /**
     * Returns the channel name for a given channel index
     *
     * @param channelIndex the channel index
     * @return the channel name
     */
    String getChannelName(int channelIndex);

    /**
     * Returns the channel name for a given channel index
     *
     * @param channelIndex the channel index
     * @return the channel name
     */
    String getChannelShortName(int channelIndex);

    /**
     * Converts an array of channel values into a pixel
     *
     * @param channelValues the channel values. must have at least getNChannels() items
     * @return the pixel
     */
    int composePixel(int... channelValues);

    /**
     * Decomposes an integer pixel value into its channel values
     *
     * @param pixel         the pixel
     * @param channelValues array with at least getNChannels() items. The values will be written into this array.
     */
    void decomposePixel(int pixel, int[] channelValues);
}
