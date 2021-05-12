package org.hkijena.jipipe.extensions.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Yen)", description = "Calculates a threshold from a " +
        "histogram using the Yen algorithm.")
public class HistogramThresholdYen extends HistogramThresholdFunction {
    public HistogramThresholdYen() {
        super("HISTOGRAM_THRESHOLD_YEN");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Yen, histogram);
    }
}
