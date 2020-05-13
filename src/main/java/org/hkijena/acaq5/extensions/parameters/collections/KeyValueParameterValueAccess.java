package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Parameter access for the key entry in {@link KeyValuePairParameter}
 */
public class KeyValueParameterValueAccess<K, V> implements ACAQParameterAccess {
    private ACAQParameterAccess parent;
    private KeyValuePairParameter<K, V> keyValuePairParameter;

    /**
     * Creates a new instance
     *
     * @param parent                the parent access
     * @param keyValuePairParameter the parameter
     */
    public KeyValueParameterValueAccess(ACAQParameterAccess parent, KeyValuePairParameter<K, V> keyValuePairParameter) {
        this.parent = parent;
        this.keyValuePairParameter = keyValuePairParameter;
    }

    public KeyValuePairParameter<K, V> getKeyValuePairParameter() {
        return keyValuePairParameter;
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
        return null;
    }

    @Override
    public Class<?> getFieldClass() {
        return keyValuePairParameter.getValueClass();
    }

    @Override
    public <T> T get(Class<T> klass) {
        return (T) keyValuePairParameter.getValue();
    }

    @Override
    public <T> boolean set(T value) {
        keyValuePairParameter.setValue((V) value);
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
