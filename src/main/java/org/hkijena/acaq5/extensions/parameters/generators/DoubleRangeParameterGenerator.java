package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Double}
 */
public class DoubleRangeParameterGenerator extends NumberRangeParameterGenerator {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public DoubleRangeParameterGenerator(Context context) {
        super(context, Double.class);
    }
}
