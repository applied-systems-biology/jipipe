package org.hkijena.jipipe.extensions.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Percentile)", description = "Calculates a threshold from a " +
        "histogram using the Percentile algorithm.")
public class HistogramThresholdPercentile extends HistogramThresholdFunction {
    public HistogramThresholdPercentile() {
        super("HISTOGRAM_THRESHOLD_PERCENTILE");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Percentile, histogram);
    }
}
