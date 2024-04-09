/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.expressions.functions.statistics;

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
