/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.ui.ACAQWorkbench;

/**
 * Generates {@link Double}
 */
public class DoubleRangeParameterGenerator extends NumberRangeParameterGenerator<Double> {

    /**
     * Creates a new instance
     *
     * @param workbench the SciJava context
     */
    public DoubleRangeParameterGenerator(ACAQWorkbench workbench) {
        super(workbench, Byte.class);
    }
}
