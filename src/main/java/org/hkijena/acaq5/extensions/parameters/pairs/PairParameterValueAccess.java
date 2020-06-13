package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Parameter access for the key entry in {@link Pair}
 */
public class PairParameterValueAccess<K, V> implements ACAQParameterAccess {
    private ACAQParameterAccess parent;
    private Pair<K, V> pair;

    /**
     * Creates a new instance
     *
     * @param parent the parent access
     * @param pair   the parameter
     */
    public PairParameterValueAccess(ACAQParameterAccess parent, Pair<K, V> pair) {
        this.parent = parent;
        this.pair = pair;
    }

    public Pair<K, V> getPair() {
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
    public ACAQParameterVisibility getVisibility() {
        return ACAQParameterVisibility.TransitiveVisible;
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
