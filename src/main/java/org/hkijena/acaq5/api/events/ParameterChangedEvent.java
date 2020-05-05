package org.hkijena.acaq5.api.events;

/**
 * Triggered when a parameter holder's parameters are changed
 */
public class ParameterChangedEvent {
    private Object source;
    private String key;

    /**
     * @param source event source
     * @param key    parameter key
     */
    public ParameterChangedEvent(Object source, String key) {
        this.source = source;
        this.key = key;
    }

    public Object getSource() {
        return source;
    }

    public String getKey() {
        return key;
    }
}
