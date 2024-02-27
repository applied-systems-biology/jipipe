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

package org.hkijena.jipipe.extensions.expressions.functions.statistics;

import ij.process.AutoThresholder;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;

@SetJIPipeDocumentation(name = "Histogram threshold (ImageJ default)", description = "Calculates a threshold from a " +
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
