package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQSlotConfiguration;

public class SlotOrderChangedEvent {
    private ACAQSlotConfiguration configuration;

    public SlotOrderChangedEvent(ACAQSlotConfiguration configuration) {
        this.configuration = configuration;
    }

    public ACAQSlotConfiguration getConfiguration() {
        return configuration;
    }
}
