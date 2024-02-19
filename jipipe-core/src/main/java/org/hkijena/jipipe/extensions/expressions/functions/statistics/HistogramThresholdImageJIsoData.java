package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

@SetJIPipeDocumentation(name = "Histogram threshold (ImageJ IsoData)", description = "Calculates a threshold from a " +
        "histogram using the IsoData algorithm.")
public class HistogramThresholdImageJIsoData extends HistogramThresholdFunction {
    public HistogramThresholdImageJIsoData() {
        super("HISTOGRAM_THRESHOLD_IJ_ISODATA");
    }

    @Override
    protected int calculateThreshold(int[] histogram) {
        return AUTO_THRESHOLDER.getThreshold(AutoThresholder.Method.IJ_IsoData, histogram);
    }
}
