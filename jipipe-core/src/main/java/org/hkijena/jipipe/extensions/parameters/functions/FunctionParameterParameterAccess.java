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

package org.hkijena.jipipe.extensions.parameters.functions;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Access to a {@link FunctionParameter} parameter
 *
 * @param <I> input type
 * @param <P> parameter type
 * @param <O> output type
 */
public class FunctionParameterParameterAccess<I, P, O> implements JIPipeParameterAccess {

    private JIPipeParameterAccess parent;
    private FunctionParameter<I, P, O> functionParameter;

    public FunctionParameterParameterAccess(JIPipeParameterAccess parent) {
        this.parent = parent;
        this.functionParameter = parent.get(FunctionParameter.class);
    }

    @Override
    public String getKey() {
        return "parameter";
    }

    @Override
    public String getName() {
        return "Parameter";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public JIPipeParameterVisibility getVisibility() {
        return JIPipeParameterVisibility.TransitiveVisible;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return parent.getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return functionParameter.getParameterClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) functionParameter.getParameter();
    }

    @Override
    public <T> boolean set(T value) {
        functionParameter.setParameter((P) value);
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
