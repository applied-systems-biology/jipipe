/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.api.functions;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Access to a {@link FunctionParameter} output
 *
 * @param <I> input type
 * @param <P> parameter type
 * @param <O> output type
 */
public class FunctionParameterOutputAccess<I, P, O> implements JIPipeParameterAccess {

    private final JIPipeParameterAccess parent;
    private final FunctionParameter<I, P, O> functionParameter;

    public FunctionParameterOutputAccess(JIPipeParameterAccess parent) {
        this.parent = parent;
        this.functionParameter = parent.get(FunctionParameter.class);
    }

    @Override
    public String getKey() {
        return parent.getKey() + "/output";
    }

    @Override
    public String getName() {
        return "Output";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return parent.getAnnotationOfType(klass);
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return parent.getAnnotations();
    }

    @Override
    public Class<?> getFieldClass() {
        return functionParameter.getOutputClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) functionParameter.getOutput();
    }

    @Override
    public <T> boolean set(T value) {
        functionParameter.setOutput((O) value);
        return true;
    }

    @Override
    public JIPipeParameterCollection getSource() {
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

    public JIPipeParameterAccess getParent() {
        return parent;
    }
}
