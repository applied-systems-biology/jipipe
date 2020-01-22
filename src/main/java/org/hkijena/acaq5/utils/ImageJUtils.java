package org.hkijena.acaq5.utils;

import ij.IJ;
import ij.ImagePlus;

public class ImageJUtils {
    private ImageJUtils() {

    }

    public static ImagePlus runOnImage(ImagePlus img, String command, String... parameters) {
        String params = String.join(" ", parameters);
        img.show();
        IJ.run(command, params);
        img.hide();
        return img;
    }

    public static ImagePlus runOnNewImage(ImagePlus img, String command, String... parameters) {
        ImagePlus copy = img.duplicate();
        String params = String.join(" ", parameters);
        copy.show();
        IJ.run(command, params);
        copy.hide();
        return copy;
    }
}
