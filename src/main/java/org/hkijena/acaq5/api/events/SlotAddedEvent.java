package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQSlotConfiguration;

public class SlotAddedEvent {
    private ACAQSlotConfiguration slotConfiguration;
    private String slotName;

    public SlotAddedEvent(ACAQSlotConfiguration slotConfiguration, String slotName) {
        this.slotConfiguration = slotConfiguration;
        this.slotName = slotName;
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public String getSlotName() {
        return slotName;
    }
}
