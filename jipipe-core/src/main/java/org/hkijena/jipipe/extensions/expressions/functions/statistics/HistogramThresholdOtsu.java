package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

@SetJIPipeDocumentation(name = "Histogram threshold (Otsu)", description = "Calculates a threshold from a " +
        "histogram using the Otsu algorithm.")
public class HistogramThresholdOtsu extends HistogramThresholdFunction {
    public HistogramThresholdOtsu() {
        super("HISTOGRAM_THRESHOLD_OTSU");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Otsu, histogram);
    }
}
