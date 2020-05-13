package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Float}
 */
public class FloatPrimitiveParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return 0.0f;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "primitive.float";
    }

    @Override
    public Class<?> getFieldClass() {
        return float.class;
    }

    @Override
    public String getName() {
        return "Floating point number (single precision)";
    }

    @Override
    public String getDescription() {
        return "A 32-bit floating point number";
    }
}
