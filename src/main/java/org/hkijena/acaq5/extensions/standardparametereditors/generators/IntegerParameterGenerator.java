package org.hkijena.acaq5.extensions.standardparametereditors.generators;

import org.scijava.Context;

/**
 * Generates {@link Integer}
 */
public class IntegerParameterGenerator extends NumberParameterGenerator {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public IntegerParameterGenerator(Context context) {
        super(context, Integer.class);
    }
}
