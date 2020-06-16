package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.scijava.Context;

/**
 * Generates {@link Long}
 */
public class LongRangeParameterGenerator extends NumberRangeParameterGenerator<Long> {

    /**
     * Creates a new instance
     *
     * @param workbench the SciJava context
     */
    public LongRangeParameterGenerator(ACAQWorkbench workbench) {
        super(workbench, Byte.class);
    }
}
