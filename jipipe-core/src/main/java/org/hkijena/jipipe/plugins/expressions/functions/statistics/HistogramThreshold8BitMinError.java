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

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;

@SetJIPipeDocumentation(name = "Histogram threshold 8-bit (Min Error)", description = "Calculates a threshold from a " +
        "256-bin histogram (bin index = pixel value) using the Min Error algorithm.")
public class HistogramThreshold8BitMinError extends HistogramThreshold8BitFunction {
    public HistogramThreshold8BitMinError() {
        super("HISTOGRAM_THRESHOLD_8_BIT_MINERROR");
    }

    @Override
    protected AutoThresholdMethod getMethod() {
        return AutoThresholdMethod.MinError;
    }
}
