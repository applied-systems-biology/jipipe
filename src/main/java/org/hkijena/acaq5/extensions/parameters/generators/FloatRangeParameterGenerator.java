package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Float}
 */
public class FloatRangeParameterGenerator extends NumberRangeParameterGenerator<Float> {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public FloatRangeParameterGenerator(Context context) {
        super(context, Float.class);
    }
}
