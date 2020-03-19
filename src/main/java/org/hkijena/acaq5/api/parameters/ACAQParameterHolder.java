package org.hkijena.acaq5.api.parameters;

import com.google.common.eventbus.EventBus;

public interface ACAQParameterHolder {
    /**
     * Gets the event bus that posts events about the parameters
     *
     * @return
     */
    EventBus getEventBus();
}
