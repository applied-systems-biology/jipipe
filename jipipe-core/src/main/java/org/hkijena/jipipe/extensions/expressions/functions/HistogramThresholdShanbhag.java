package org.hkijena.jipipe.extensions.expressions.functions;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;

@JIPipeDocumentation(name = "Histogram threshold (Shanbhag)", description = "Calculates a threshold from a " +
        "histogram using the Shanbhag algorithm.")
public class HistogramThresholdShanbhag extends HistogramThresholdFunction {
    public HistogramThresholdShanbhag() {
        super("HISTOGRAM_THRESHOLD_SHANBHAG");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Shanbhag, histogram);
    }
}
