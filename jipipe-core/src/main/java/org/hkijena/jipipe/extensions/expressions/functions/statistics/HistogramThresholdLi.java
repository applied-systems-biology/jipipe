package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

@SetJIPipeDocumentation(name = "Histogram threshold (Li)", description = "Calculates a threshold from a " +
        "histogram using the Li algorithm.")
public class HistogramThresholdLi extends HistogramThresholdFunction {
    public HistogramThresholdLi() {
        super("HISTOGRAM_THRESHOLD_LI");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Li, histogram);
    }
}
