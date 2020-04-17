package org.hkijena.acaq5.extensions.standardparametereditors.generators;

import org.scijava.Context;

/**
 * Generates {@link Short}
 */
public class ShortParameterGenerator extends NumberParameterGenerator {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public ShortParameterGenerator(Context context) {
        super(context, Short.class);
    }
}
