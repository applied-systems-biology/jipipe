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
