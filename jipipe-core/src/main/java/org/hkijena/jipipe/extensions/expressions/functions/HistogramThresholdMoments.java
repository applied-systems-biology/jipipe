package org.hkijena.jipipe.extensions.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Moments)", description = "Calculates a threshold from a " +
        "histogram using the Moments algorithm.")
public class HistogramThresholdMoments extends HistogramThresholdFunction {
    public HistogramThresholdMoments() {
        super("HISTOGRAM_THRESHOLD_MOMENTS");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Moments, histogram);
    }
}
