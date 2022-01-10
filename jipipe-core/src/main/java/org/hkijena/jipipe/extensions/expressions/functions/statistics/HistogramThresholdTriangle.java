package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Triangle)", description = "Calculates a threshold from a " +
        "histogram using the Triangle algorithm.")
public class HistogramThresholdTriangle extends HistogramThresholdFunction {
    public HistogramThresholdTriangle() {
        super("HISTOGRAM_THRESHOLD_TRIANGLE");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Triangle, histogram);
    }
}