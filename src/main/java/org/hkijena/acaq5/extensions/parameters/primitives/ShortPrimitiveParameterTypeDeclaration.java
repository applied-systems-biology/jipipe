package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Short}
 */
public class ShortPrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return (short) 0;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.short";
    }

    @Override
    public Class<?> getFieldClass() {
        return short.class;
    }

    @Override
    public String getName() {
        return "16-bit integral number";
    }

    @Override
    public String getDescription() {
        return "An integral number ranging from " + Short.MIN_VALUE + " to " + Short.MAX_VALUE;
    }
}
