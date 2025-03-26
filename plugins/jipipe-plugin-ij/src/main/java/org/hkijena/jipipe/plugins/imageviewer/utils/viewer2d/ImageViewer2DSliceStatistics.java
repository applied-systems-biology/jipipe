package org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d;

import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJHistogram;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

public class ImageViewer2DSliceStatistics {
    private final ImageStatistics imageStatistics;
    private final ImageJHistogram histogram;

    public ImageViewer2DSliceStatistics(ImageStatistics imageStatistics, ImageJHistogram histogram) {
        this.imageStatistics = imageStatistics;
        this.histogram = histogram;
    }

    public ImageViewer2DSliceStatistics(ImageProcessor imageProcessor) {
        this(imageProcessor.getStatistics(), ImageJUtils.computeGrayscaleHistogram(imageProcessor));
    }

    public ImageStatistics getImageStatistics() {
        return imageStatistics;
    }

    public ImageJHistogram getHistogram() {
        return histogram;
    }
}
