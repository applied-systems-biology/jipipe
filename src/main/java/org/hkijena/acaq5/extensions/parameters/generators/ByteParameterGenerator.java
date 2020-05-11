package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Byte}
 */
public class ByteParameterGenerator extends NumberParameterGenerator {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public ByteParameterGenerator(Context context) {
        super(context, Byte.class);
    }
}
