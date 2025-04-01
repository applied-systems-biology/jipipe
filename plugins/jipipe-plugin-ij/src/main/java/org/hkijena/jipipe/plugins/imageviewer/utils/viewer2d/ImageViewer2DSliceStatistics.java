package org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJHistogram;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

public class ImageViewer2DSliceStatistics {
    private final ImageProcessor imageProcessor;
    private ImageStatistics fastImageStatistics;
    private ImageStatistics slowImageStatistics;
    private ImageJHistogram histogram;

    public ImageViewer2DSliceStatistics(ImageProcessor imageProcessor) {
        this.imageProcessor = imageProcessor;
    }

    public ImageStatistics getFastImageStatistics() {
        if (fastImageStatistics == null) {
            fastImageStatistics = imageProcessor.getStats();
        }
        return fastImageStatistics;
    }

    public ImageProcessor getImageProcessor() {
        return imageProcessor;
    }

    public ImageStatistics getSlowImageStatistics() {
        if (slowImageStatistics == null) {
            slowImageStatistics = imageProcessor.getStatistics();
        }
        return slowImageStatistics;
    }

    public ImageJHistogram getHistogram() {
        if (histogram == null) {
            histogram = ImageJUtils.computeGrayscaleHistogram(imageProcessor);
        }
        return histogram;
    }
}
