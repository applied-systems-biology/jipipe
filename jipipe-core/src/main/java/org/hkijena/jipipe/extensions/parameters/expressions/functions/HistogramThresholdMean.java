package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Mean)", description = "Calculates a threshold from a " +
        "histogram using the Mean algorithm.")
public class HistogramThresholdMean extends HistogramThresholdFunction {
    public HistogramThresholdMean() {
        super("HISTOGRAM_THRESHOLD_MEAN");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Mean, histogram);
    }
}
