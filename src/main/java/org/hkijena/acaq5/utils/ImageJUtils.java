package org.hkijena.acaq5.utils;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ImageJUtils {
    private ImageJUtils() {

    }

    public static ImagePlus runOnImage(ImagePlus img, String command, Object... parameters) {
        String params = toParameterString(parameters);
        img.show();
        IJ.run(command, params);
        img.hide();
        return img;
    }

    public static ImagePlus runOnNewImage(ImagePlus img, String command, Object... parameters) {
        ImagePlus copy = img.duplicate();
        String params = toParameterString(parameters);
        copy.show();
        IJ.run(command, params);
        copy.hide();
        return copy;
    }

    public static ImagePlus runOnImage(ImagePlus img, PlugIn plugin, Object... parameters) {
        String params = toParameterString(parameters);
        img.show();
        plugin.run(params);
        img.hide();
        return img;
    }

    public static ImagePlus runOnNewImage(ImagePlus img, PlugIn plugin, Object... parameters) {
        ImagePlus copy = img.duplicate();
        String params = toParameterString(parameters);
        copy.show();
        plugin.run(params);
        copy.hide();
        return copy;
    }

    public static String toParameterString(Object... parameters) {
        return Arrays.stream(parameters).map(Object::toString).collect(Collectors.joining(" "));
    }
}
