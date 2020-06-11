package org.hkijena.acaq5.extensions.parameters.optional;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;
import org.hkijena.acaq5.extensions.parameters.pairs.Pair;

import java.lang.annotation.Annotation;

/**
 * Parameter access for the key entry in {@link Pair}
 */
public class OptionalParameterContentAccess<T> implements ACAQParameterAccess {
    private ACAQParameterAccess parent;
    private OptionalParameter<T> optionalParameter;

    /**
     * Creates a new instance
     *
     * @param parent            the parent access
     * @param optionalParameter the parameter
     */
    public OptionalParameterContentAccess(ACAQParameterAccess parent, OptionalParameter<T> optionalParameter) {
        this.parent = parent;
        this.optionalParameter = optionalParameter;
    }

    public OptionalParameter<T> getOptionalParameter() {
        return optionalParameter;
    }

    @Override
    public String getKey() {
        return "content";
    }

    @Override
    public String getName() {
        return "Content";
    }

    @Override
    public String getDescription() {
        return "Parameter content";
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return ACAQParameterVisibility.TransitiveVisible;
    }

    @Override
    public <U extends Annotation> U getAnnotationOfType(Class<U> klass) {
        return parent.getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return optionalParameter.getContentClass();
    }

    @Override
    public <U> U get(Class<U> klass) {
        return (U) optionalParameter.getContent();
    }

    @Override
    public <U> boolean set(U value) {
        optionalParameter.setContent((T) value);
        return true;
    }

    @Override
    public ACAQParameterCollection getSource() {
        return parent.getSource();
    }

    @Override
    public double getPriority() {
        return 0;
    }

    @Override
    public String getShortKey() {
        return null;
    }

    @Override
    public int getUIOrder() {
        return 0;
    }

    public ACAQParameterAccess getParent() {
        return parent;
    }
}
