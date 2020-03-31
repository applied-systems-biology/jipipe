package org.hkijena.acaq5.api.parameters;

import com.google.common.eventbus.EventBus;

/**
 * Interfaced for a parameterized object
 */
public interface ACAQParameterHolder {
    /**
     * Gets the event bus that posts events about the parameters
     *
     * @return The event bus triggering {@link org.hkijena.acaq5.api.events.ParameterChangedEvent} and {@link org.hkijena.acaq5.api.events.ParameterStructureChangedEvent}
     */
    EventBus getEventBus();
}
