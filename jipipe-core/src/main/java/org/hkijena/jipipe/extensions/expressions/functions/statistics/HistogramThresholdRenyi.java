package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

@SetJIPipeDocumentation(name = "Histogram threshold (Renyi Entropy)", description = "Calculates a threshold from a " +
        "histogram using the Renyi entropy algorithm.")
public class HistogramThresholdRenyi extends HistogramThresholdFunction {
    public HistogramThresholdRenyi() {
        super("HISTOGRAM_THRESHOLD_RENYI");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.RenyiEntropy, histogram);
    }
}
