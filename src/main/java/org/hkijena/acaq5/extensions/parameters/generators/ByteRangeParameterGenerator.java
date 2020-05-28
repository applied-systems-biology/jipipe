package org.hkijena.acaq5.extensions.parameters.generators;

import org.scijava.Context;

/**
 * Generates {@link Byte}
 */
public class ByteRangeParameterGenerator extends NumberRangeParameterGenerator<Byte> {

    /**
     * Creates a new instance
     *
     * @param context the SciJava context
     */
    public ByteRangeParameterGenerator(Context context) {
        super(context, Byte.class);
    }
}
