package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQSlotConfiguration;

public class SlotRemovedEvent {
    private ACAQSlotConfiguration slotConfiguration;
    private String slotName;

    public SlotRemovedEvent(ACAQSlotConfiguration slotConfiguration, String slotName) {
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
