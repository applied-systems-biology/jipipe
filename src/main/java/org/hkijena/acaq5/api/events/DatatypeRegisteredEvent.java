package org.hkijena.acaq5.api.events;

/**
 * Triggered when a new data type is registered
 */
public class DatatypeRegisteredEvent {
    private String id;

    /**
     * @param id the data type id
     */
    public DatatypeRegisteredEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
