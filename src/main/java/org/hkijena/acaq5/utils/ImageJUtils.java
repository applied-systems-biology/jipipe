package org.hkijena.acaq5.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.util.Arrays;
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
