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

package org.hkijena.jipipe.extensions.parameters.api.optional;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameter;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

/**
 * Parameter access for the key entry in {@link PairParameter}
 */
public class OptionalParameterContentAccess<T> implements JIPipeParameterAccess {
    private final JIPipeParameterAccess parent;
    private final OptionalParameter<T> optionalParameter;

    private final JIPipeParameterCollection.ParameterChangedEventEmitter parameterChangedEventEmitter = new JIPipeParameterCollection.ParameterChangedEventEmitter();

    /**
     * Creates a new instance
     *
     * @param parent            the parent access
     * @param optionalParameter the parameter
     */
    public OptionalParameterContentAccess(JIPipeParameterAccess parent, OptionalParameter<T> optionalParameter) {
        this.parent = parent;
        this.optionalParameter = optionalParameter;
    }

    public OptionalParameter<T> getOptionalParameter() {
        return optionalParameter;
    }

    public JIPipeParameterCollection.ParameterChangedEventEmitter getParameterChangedEventEmitter() {
        return parameterChangedEventEmitter;
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
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isImportant() {
        return false;
    }

    @Override
    public <U extends Annotation> U getAnnotationOfType(Class<U> klass) {
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
        return optionalParameter.getContentClass();
    }

    @Override
    public <U> U get(Class<U> klass) {
        return (U) optionalParameter.getContent();
    }

    @Override
    public <U> boolean set(U value) {
        optionalParameter.setContent((T) value);
        parameterChangedEventEmitter.emit(new JIPipeParameterCollection.ParameterChangedEvent(this, "content"));
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


    public JIPipeParameterAccess getParent() {
        return parent;
    }
}
