package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

@JIPipeDocumentation(name = "Histogram threshold (Intermodes)", description = "Calculates a threshold from a " +
        "histogram using the Intermodes algorithm.")
public class HistogramThresholdIntermodes extends HistogramThresholdFunction {
    public HistogramThresholdIntermodes() {
        super("HISTOGRAM_THRESHOLD_INTERMODES");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(new JIPipeProgressInfo())) {
            return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.Intermodes, histogram);
        }
    }
}
