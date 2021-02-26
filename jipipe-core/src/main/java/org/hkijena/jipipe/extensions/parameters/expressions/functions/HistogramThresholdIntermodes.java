package org.hkijena.jipipe.extensions.parameters.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Intermodes)", description = "Calculates a threshold from a " +
        "histogram using the Intermodes algorithm.")
public class HistogramThresholdIntermodes extends HistogramThresholdFunction {
    public HistogramThresholdIntermodes() {
        super("HISTOGRAM_THRESHOLD_INTERMODES");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Intermodes, histogram);
    }
}
