package org.hkijena.jipipe.extensions.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Minimum)", description = "Calculates a threshold from a " +
        "histogram using the Minium algorithm.")
public class HistogramThresholdMinimum extends HistogramThresholdFunction {
    public HistogramThresholdMinimum() {
        super("HISTOGRAM_THRESHOLD_MINIMUM");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Minimum, histogram);
    }
}
