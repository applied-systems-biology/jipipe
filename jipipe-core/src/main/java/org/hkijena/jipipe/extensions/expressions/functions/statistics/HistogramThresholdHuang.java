package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

@SetJIPipeDocumentation(name = "Histogram threshold (Huang)", description = "Calculates a threshold from a " +
        "histogram using the Huang algorithm.")
public class HistogramThresholdHuang extends HistogramThresholdFunction {
    public HistogramThresholdHuang() {
        super("HISTOGRAM_THRESHOLD_HUANG");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Huang, histogram);
    }
}
