package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Long}
 */
public class LongPrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return 0L;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.long";
    }

    @Override
    public Class<?> getFieldClass() {
        return long.class;
    }

    @Override
    public String getName() {
        return "64-bit integral number";
    }

    @Override
    public String getDescription() {
        return "An integral number ranging from " + Long.MIN_VALUE + " to " + Long.MAX_VALUE;
    }
}
