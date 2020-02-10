package org.hkijena.acaq5.api;

import com.google.common.eventbus.EventBus;

import java.util.Map;

public abstract class ACAQSlotConfiguration {
    private EventBus eventBus = new EventBus();
    public abstract Map<String, ACAQSlotDefinition> getSlots();
    public EventBus getEventBus() {
        return eventBus;
    }
}
