package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (IsoData)", description = "Calculates a threshold from a " +
        "histogram using the IsoData algorithm.")
public class HistogramThresholdIsoData extends HistogramThresholdFunction {
    public HistogramThresholdIsoData() {
        super("HISTOGRAM_THRESHOLD_ISODATA");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.IsoData, histogram);
    }
}
