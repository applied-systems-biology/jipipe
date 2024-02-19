package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

@SetJIPipeDocumentation(name = "Histogram threshold (Minimum)", description = "Calculates a threshold from a " +
        "histogram using the Minium algorithm.")
public class HistogramThresholdMinimum extends HistogramThresholdFunction {
    public HistogramThresholdMinimum() {
        super("HISTOGRAM_THRESHOLD_MINIMUM");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(new JIPipeProgressInfo())) {
            return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Minimum, histogram);
        }
    }
}
