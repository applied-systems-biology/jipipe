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

package org.hkijena.jipipe.extensions.parameters.pairs;

import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Parameter access for the key entry in {@link PairParameter}
 */
public class PairParameterValueAccess<K, V> implements JIPipeParameterAccess {
    private JIPipeParameterAccess parent;
    private PairParameter<K, V> pair;

    /**
     * Creates a new instance
     *
     * @param parent the parent access
     * @param pair   the parameter
     */
    public PairParameterValueAccess(JIPipeParameterAccess parent, PairParameter<K, V> pair) {
        this.parent = parent;
        this.pair = pair;
    }

    public PairParameter<K, V> getPair() {
        return pair;
    }

    @Override
    public String getKey() {
        return "value";
    }

    @Override
    public String getName() {
        return "Value";
    }

    @Override
    public String getDescription() {
        return "Parameter value";
    }

    @Override
    public JIPipeParameterVisibility getVisibility() {
        return JIPipeParameterVisibility.TransitiveVisible;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return getParent().getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return pair.getValueClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) pair.getValue();
    }

    @Override
    public <T> boolean set(T value) {
        pair.setValue((V) value);
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
