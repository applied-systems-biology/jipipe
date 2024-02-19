package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

@SetJIPipeDocumentation(name = "Histogram threshold (Min Error)", description = "Calculates a threshold from a " +
        "histogram using the Min Error algorithm.")
public class HistogramThresholdMinError extends HistogramThresholdFunction {
    public HistogramThresholdMinError() {
        super("HISTOGRAM_THRESHOLD_MIN_ERROR");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(new JIPipeProgressInfo())) {
            return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.MinError, histogram);
        }
    }
}
