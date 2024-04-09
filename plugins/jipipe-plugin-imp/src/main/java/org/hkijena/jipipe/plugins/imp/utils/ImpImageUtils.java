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

package org.hkijena.jipipe.plugins.imp.utils;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imp.datatypes.ImpImageData;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiConsumer;

public class ImpImageUtils {
    private ImpImageUtils() {

    }


    public static int findNSlices(Map<ImageSliceIndex, BufferedImage> images) {
        return images.keySet().stream().map(ImageSliceIndex::getZ).max(Comparator.naturalOrder()).orElse(-1) + 1;
    }

    public static int findNFrames(Map<ImageSliceIndex, BufferedImage> images) {
        return images.keySet().stream().map(ImageSliceIndex::getT).max(Comparator.naturalOrder()).orElse(-1) + 1;
    }

    public static int findNChannels(Map<ImageSliceIndex, BufferedImage> images) {
        return images.keySet().stream().map(ImageSliceIndex::getC).max(Comparator.naturalOrder()).orElse(-1) + 1;
    }

    public static int findWidth(Collection<BufferedImage> images) {
        int width = 0;
        for (BufferedImage image : images) {
            if (width != 0 && width != image.getWidth()) {
                throw new IllegalArgumentException("Inconsistent image width!");
            }
            width = image.getWidth();
        }
        return width;
    }

    public static int findHeight(Collection<BufferedImage> images) {
        int height = 0;
        for (BufferedImage image : images) {
            if (height != 0 && height != image.getHeight()) {
                throw new IllegalArgumentException("Inconsistent image height!");
            }
            height = image.getHeight();
        }
        return height;
    }

    /**
     * Runs the function for each Z, C, and T slice.
     *
     * @param img          the image
     * @param function     the function. The indices are ZERO-based
     * @param progressInfo the progress
     */
    public static void forEachIndexedZCTSlice(ImpImageData img, BiConsumer<BufferedImage, ImageSliceIndex> function, JIPipeProgressInfo progressInfo) {
        if (img.getImages().size() > 1) {
            int iterationIndex = 0;
            for (int t = 0; t < img.getNumFrames(); t++) {
                for (int z = 0; z < img.getNumSlices(); z++) {
                    for (int c = 0; c < img.getNumChannels(); c++) {
                        if (progressInfo.isCancelled())
                            return;
                        progressInfo.resolveAndLog("Slice", iterationIndex++, img.getSize()).log("z=" + z + ", c=" + c + ", t=" + t);
                        BufferedImage processor = img.getImage(c, z, t);
                        function.accept(processor, new ImageSliceIndex(c, z, t));
                    }
                }
            }
        } else {
            function.accept(img.getImage(0), new ImageSliceIndex(0, 0, 0));
        }
    }
}
