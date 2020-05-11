package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Double}
 */
public class DoubleParameterGenerator extends NumberParameterGenerator {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public DoubleParameterGenerator(Context context) {
        super(context, Double.class);
    }
}
