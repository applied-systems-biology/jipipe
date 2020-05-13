package org.hkijena.acaq5.extensions.parameters.primitives;

import org.hkijena.acaq5.api.parameters.ACAQParameterTypeDeclaration;

/**
 * Declaration for {@link Boolean}
 */
public class BooleanParameterTypeDeclaration implements ACAQParameterTypeDeclaration {
    @Override
    public Object newInstance() {
        return false;
    }

    @Override
    public Object duplicate(Object original) {
        return original;
    }

    @Override
    public String getId() {
        return "boolean";
    }

    @Override
    public Class<?> getFieldClass() {
        return Boolean.class;
    }

    @Override
    public String getName() {
        return "Boolean value";
    }

    @Override
    public String getDescription() {
        return "A value that can be true or false";
    }
}
