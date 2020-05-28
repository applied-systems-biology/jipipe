package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Integer}
 */
public class IntegerRangeParameterGenerator extends NumberRangeParameterGenerator<Integer> {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public IntegerRangeParameterGenerator(Context context) {
        super(context, Integer.class);
    }
}
