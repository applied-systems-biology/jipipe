package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Integer}
 */
public class IntPrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return 0;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.int";
    }

    @Override
    public Class<?> getFieldClass() {
        return int.class;
    }

    @Override
    public String getName() {
        return "32-bit integral number";
    }

    @Override
    public String getDescription() {
        return "An integral number ranging from " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE;
    }
}
