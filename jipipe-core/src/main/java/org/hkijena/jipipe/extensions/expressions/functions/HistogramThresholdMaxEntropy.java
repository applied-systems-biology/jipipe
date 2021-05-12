package org.hkijena.jipipe.extensions.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Max Entropy)", description = "Calculates a threshold from a " +
        "histogram using the Max Entropy algorithm.")
public class HistogramThresholdMaxEntropy extends HistogramThresholdFunction {
    public HistogramThresholdMaxEntropy() {
        super("HISTOGRAM_THRESHOLD_MAX_ENTROPY");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.MaxEntropy, histogram);
    }
}
