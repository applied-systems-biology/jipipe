package org.hkijena.acaq5.extensions.parameters.generators;

import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.scijava.Context;

/**
 * Generates {@link Byte}
 */
public class ByteRangeParameterGenerator extends NumberRangeParameterGenerator<Byte> {

    /**
     * Creates a new instance
     *
     * @param workbench the SciJava context
     */
    public ByteRangeParameterGenerator(ACAQWorkbench workbench) {
        super(workbench, Byte.class);
    }
}
