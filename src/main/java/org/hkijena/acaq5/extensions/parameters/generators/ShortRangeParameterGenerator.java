package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Short}
 */
public class ShortRangeParameterGenerator extends NumberRangeParameterGenerator {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public ShortRangeParameterGenerator(Context context) {
        super(context, Short.class);
    }
}
