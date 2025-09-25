/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.api.functions;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Access to a {@link JIPipeFunctionParameter} parameter
 *
 * @param <I> input type
 * @param <P> parameter type
 * @param <O> output type
 */
public class JIPipeFunctionParameterParameterAccess<I, P, O> implements JIPipeParameterAccess {

    private final JIPipeParameterAccess parent;
    private final JIPipeFunctionParameter<I, P, O> functionParameter;

    public JIPipeFunctionParameterParameterAccess(JIPipeParameterAccess parent) {
        this.parent = parent;
        this.functionParameter = parent.get(JIPipeFunctionParameter.class);
    }

    @Override
    public String getKey() {
        return parent.getKey() + "/parameter";
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
    public <T extends Annotation> List<T> getAnnotationsOfType(Class<T> klass) {
        return getParent().getAnnotationsOfType(klass);
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return parent.getAnnotations();
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

    public JIPipeFunctionParameter<I, P, O> getFunctionParameter() {
        return functionParameter;
    }

    public JIPipeParameterAccess getParent() {
        return parent;
    }
}
