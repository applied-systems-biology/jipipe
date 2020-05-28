package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Long}
 */
public class LongRangeParameterGenerator extends NumberRangeParameterGenerator<Long> {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public LongRangeParameterGenerator(Context context) {
        super(context, Long.class);
    }
}
