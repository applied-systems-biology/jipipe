package org.hkijena.acaq5.api.events;

/**
 * Triggered when a parameter holder's parameters are changed
 */
public class ParameterChangedEvent {
    private Object parameterHolder;
    private String key;

    /**
     * @param parameterHolder event source
     * @param key             parameter key
     */
    public ParameterChangedEvent(Object parameterHolder, String key) {
        this.parameterHolder = parameterHolder;
        this.key = key;
    }

    public Object getParameterHolder() {
        return parameterHolder;
    }

    public String getKey() {
        return key;
    }
}
