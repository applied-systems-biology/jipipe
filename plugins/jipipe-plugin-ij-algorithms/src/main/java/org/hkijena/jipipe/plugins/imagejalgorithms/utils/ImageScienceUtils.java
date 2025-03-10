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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.process.LUT;
import imagescience.image.Image;
import imagescience.utility.I5DResource;

public class ImageScienceUtils {

    public static final int SINGLEIMAGE = 1, IMAGESTACK = 2, HYPERSTACK = 3, COMPOSITEIMAGE = 4, IMAGE5D = 5;

    public static int type(final ImagePlus imp) {

        int type = SINGLEIMAGE;
        boolean i5dexist = false;
        try {
            Class.forName("i5d.Image5D");
            i5dexist = true;
        } catch (Throwable e) {
        }
        if (i5dexist && I5DResource.instance(imp)) type = IMAGE5D;
        else if (imp.isComposite()) type = COMPOSITEIMAGE;
        else if (imp.isHyperStack()) type = HYPERSTACK;
        else if (imp.getImageStackSize() > 1) type = IMAGESTACK;
        return type;
    }

    public static ImagePlus unwrap(Image img, ImagePlus imp) {
        ImagePlus output = img.imageplus();
        output.setCalibration(imp.getCalibration());
        final double[] minmax = img.extrema();
        final double min = minmax[0], max = minmax[1];
        output.setDisplayRange(min, max);

        switch (type(imp)) {

            case IMAGE5D: {
                output = I5DResource.convert(output, true);
                I5DResource.transfer(imp, output);
                I5DResource.minmax(output, min, max);
                I5DResource.mode(output, I5DResource.GRAY);
                break;
            }
            case COMPOSITEIMAGE: {
                final CompositeImage composite = new CompositeImage(output);
                composite.copyLuts(imp);
                composite.setMode(CompositeImage.GRAYSCALE);
                final int nc = composite.getNChannels();
                for (int c = 1; c <= nc; ++c) {
                    final LUT lut = composite.getChannelLut(c);
                    lut.min = min;
                    lut.max = max;
                }
                output = composite;
                break;
            }
            case HYPERSTACK: {
                output.setOpenAsHyperStack(true);
                break;
            }
        }

        return output;
    }
}
