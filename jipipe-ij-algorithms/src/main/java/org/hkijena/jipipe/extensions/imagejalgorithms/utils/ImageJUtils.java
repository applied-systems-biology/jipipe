/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.SliceIndex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility functions for ImageJ
 */
public class ImageJUtils {
    private ImageJUtils() {

    }

    /**
     * Runs the function for each slice
     *
     * @param img      the image
     * @param function the function
     */
    public static void forEachSlice(ImagePlus img, Consumer<ImageProcessor> function) {
        if (img.isStack()) {
            for (int i = 0; i < img.getStack().size(); ++i) {
                ImageProcessor ip = img.getStack().getProcessor(i + 1);
                function.accept(ip);
            }
        } else {
            function.accept(img.getProcessor());
        }
    }

    /**
     * Runs the function for each slice
     *
     * @param img      the image
     * @param function the function
     */
    public static void forEachIndexedSlice(ImagePlus img, BiConsumer<ImageProcessor, Integer> function) {
        if (img.isStack()) {
            for (int i = 0; i < img.getStack().size(); ++i) {
                ImageProcessor ip = img.getStack().getProcessor(i + 1);
                function.accept(ip, i);
            }
        } else {
            function.accept(img.getProcessor(), 0);
        }
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img      the image
     * @param function the function
     */
    public static void forEachIndexedZCTSlice(ImagePlus img, BiConsumer<ImageProcessor, SliceIndex> function) {
        if (img.isStack()) {
            for (int t = 0; t < img.getNFrames(); t++) {
                for (int z = 0; z < img.getNSlices(); z++) {
                    for (int c = 0; c < img.getNChannels(); c++) {
                        int index = img.getStackIndex(c + 1, z + 1, t + 1);
                        ImageProcessor processor = img.getImageStack().getProcessor(index);
                        function.accept(processor, new SliceIndex(z, c, t));
                    }
                }
            }
        }
        else {
            function.accept(img.getProcessor(), new SliceIndex(0,0,0));
        }
    }

    /**
     * Runs the function for each Z and T slice.
     * The function consumes a map from channel index to the channel slice.
     * The slice index channel is always set to -1
     *
     * @param img      the image
     * @param function the function
     */
    public static void forEachIndexedZTSlice(ImagePlus img, BiConsumer<Map<Integer, ImageProcessor>, SliceIndex> function) {
        for (int t = 0; t < img.getNFrames(); t++) {
            for (int z = 0; z < img.getNSlices(); z++) {
                Map<Integer, ImageProcessor> channels = new HashMap<>();
                for (int c = 0; c < img.getNChannels(); c++) {
                    channels.put(c, img.getStack().getProcessor(img.getStackIndex(c + 1, z + 1, t + 1)));
                }
                function.accept(channels, new SliceIndex(z, -1, t));
            }
        }
    }

    /**
     * Runs a command on an ImageJ image
     *
     * @param img        the image
     * @param command    the command
     * @param parameters command parameters
     * @return Result image
     */
    public static ImagePlus runOnImage(ImagePlus img, String command, Object... parameters) {
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(img);
        IJ.run(command, params);
        WindowManager.setTempCurrentImage(null);
        return img;
    }

    /**
     * Runs a command on a copy of an ImageJ image
     *
     * @param img        the image
     * @param command    the command
     * @param parameters the command parameters
     * @return Result image
     */
    public static ImagePlus runOnNewImage(ImagePlus img, String command, Object... parameters) {
        ImagePlus copy = img.duplicate();
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(copy);
        IJ.run(command, params);
        WindowManager.setTempCurrentImage(null);
        return copy;
    }

    /**
     * Runs a command on a copy of an ImageJ image
     *
     * @param img        the image
     * @param plugin     the command
     * @param parameters the command parameters
     * @return Result image
     */
    public static ImagePlus runOnImage(ImagePlus img, PlugIn plugin, Object... parameters) {
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(img);
        plugin.run(params);
        WindowManager.setTempCurrentImage(null);
        return img;
    }

    /**
     * Runs a command on a copy of an ImageJ image
     *
     * @param img        the image
     * @param plugin     the command
     * @param parameters the command parameters
     * @return Result image
     */
    public static ImagePlus runOnNewImage(ImagePlus img, PlugIn plugin, Object... parameters) {
        ImagePlus copy = img.duplicate();
        String params = toParameterString(parameters);
        WindowManager.setTempCurrentImage(copy);
        plugin.run(params);
        WindowManager.setTempCurrentImage(null);
        return copy;
    }

    /**
     * Converts a list of parameters into a space-delimited
     *
     * @param parameters the parameters
     * @return Joined string
     */
    public static String toParameterString(Object... parameters) {
        return Arrays.stream(parameters).map(Object::toString).collect(Collectors.joining(" "));
    }
}
