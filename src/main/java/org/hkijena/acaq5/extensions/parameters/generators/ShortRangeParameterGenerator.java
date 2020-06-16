package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.scijava.Context;

/**
 * Generates {@link Short}
 */
public class ShortRangeParameterGenerator extends NumberRangeParameterGenerator<Short> {

    /**
     * Creates a new instance
     *
     * @param workbench the SciJava context
     */
    public ShortRangeParameterGenerator(ACAQWorkbench workbench) {
        super(workbench, Byte.class);
    }
}
