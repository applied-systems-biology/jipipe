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

package org.hkijena.pipelinej.extensions.parameters.pairs;

import org.hkijena.pipelinej.api.parameters.ACAQParameterAccess;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Parameter access for the key entry in {@link Pair}
 */
public class PairParameterKeyAccess<K, V> implements ACAQParameterAccess {
    private ACAQParameterAccess parent;
    private Pair<K, V> pair;

    /**
     * Creates a new instance
     *
     * @param parent the parent access
     * @param pair   the parameter
     */
    public PairParameterKeyAccess(ACAQParameterAccess parent, Pair<K, V> pair) {
        this.parent = parent;
        this.pair = pair;
    }

    public Pair<K, V> getPair() {
        return pair;
    }

    @Override
    public String getKey() {
        return "key";
    }

    @Override
    public String getName() {
        return "Key";
    }

    @Override
    public String getDescription() {
        return "Parameter key";
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return ACAQParameterVisibility.TransitiveVisible;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return getParent().getAnnotationOfType(klass);
    }

    @Override
    public Class<?> getFieldClass() {
        return pair.getKeyClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) pair.getKey();
    }

    @Override
    public <T> boolean set(T value) {
        pair.setKey((K) value);
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
