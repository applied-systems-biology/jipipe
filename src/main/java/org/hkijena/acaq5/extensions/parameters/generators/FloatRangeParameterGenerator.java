package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.ui.ACAQWorkbench;

/**
 * Generates {@link Float}
 */
public class FloatRangeParameterGenerator extends NumberRangeParameterGenerator<Float> {

    /**
     * Creates a new instance
     *
     * @param workbench the SciJava context
     */
    public FloatRangeParameterGenerator(ACAQWorkbench workbench) {
        super(workbench, Byte.class);
    }
}
