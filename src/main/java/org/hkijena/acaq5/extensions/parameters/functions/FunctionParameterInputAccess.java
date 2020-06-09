package org.hkijena.acaq5.extensions.parameters.functions;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Access to a {@link FunctionParameter} input
 * @param <I> input type
 * @param <P> parameter type
 * @param <O> output type
 */
public class FunctionParameterInputAccess<I, P, O> implements ACAQParameterAccess {

    private ACAQParameterAccess parent;
    private FunctionParameter<I, P, O> functionParameter;

    public FunctionParameterInputAccess(ACAQParameterAccess parent) {
        this.parent = parent;
        this.functionParameter = parent.get(FunctionParameter.class);
    }

    @Override
    public String getKey() {
        return "input";
    }

    @Override
    public String getName() {
        return "Input";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return ACAQParameterVisibility.TransitiveVisible;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return parent.getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return functionParameter.getInputClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) functionParameter.getInput();
    }

    @Override
    public <T> boolean set(T value) {
        functionParameter.setInput((I) value);
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

    public FunctionParameter<I, P, O> getFunctionParameter() {
        return functionParameter;
    }

    public ACAQParameterAccess getParent() {
        return parent;
    }
}
