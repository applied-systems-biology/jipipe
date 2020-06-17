package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.ui.ACAQWorkbench;

/**
 * Generates {@link Integer}
 */
public class IntegerRangeParameterGenerator extends NumberRangeParameterGenerator<Integer> {

    /**
     * Creates a new instance
     *
     * @param workbench the SciJava context
     */
    public IntegerRangeParameterGenerator(ACAQWorkbench workbench) {
        super(workbench, Byte.class);
    }
}
