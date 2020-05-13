package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Double}
 */
public class DoublePrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return 0.0;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.double";
    }

    @Override
    public Class<?> getFieldClass() {
        return double.class;
    }

    @Override
    public String getName() {
        return "Floating point number (double precision)";
    }

    @Override
    public String getDescription() {
        return "A 64-bit floating point number";
    }
}
