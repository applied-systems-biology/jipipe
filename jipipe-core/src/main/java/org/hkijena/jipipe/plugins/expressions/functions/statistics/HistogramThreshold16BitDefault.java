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

@SetJIPipeDocumentation(name = "Histogram threshold 16-bit (ImageJ default)", description = "Calculates a threshold from a " +
        "65536-bin histogram (bin index = pixel value) using the ImageJ default algorithm.")
public class HistogramThreshold16BitDefault extends HistogramThreshold16BitFunction {
    public HistogramThreshold16BitDefault() {
        super("HISTOGRAM_THRESHOLD_16_BIT_DEFAULT");
    }

    @Override
    protected AutoThresholdMethod getMethod() {
        return AutoThresholdMethod.Default;
    }
}
