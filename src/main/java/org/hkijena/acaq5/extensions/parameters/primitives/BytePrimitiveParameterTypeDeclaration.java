package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Byte}
 */
public class BytePrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return (byte) 0;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.byte";
    }

    @Override
    public Class<?> getFieldClass() {
        return byte.class;
    }

    @Override
    public String getName() {
        return "8-bit integral number";
    }

    @Override
    public String getDescription() {
        return "An integral number ranging from " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE;
    }
}
