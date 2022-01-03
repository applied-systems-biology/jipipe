package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (ImageJ default)", description = "Calculates a threshold from a " +
        "histogram using the ImageJ default algorithm.")
public class HistogramThresholdImageJDefault extends HistogramThresholdFunction {
    public HistogramThresholdImageJDefault() {
        super("HISTOGRAM_THRESHOLD_DEFAULT");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Default, histogram);
    }
}
