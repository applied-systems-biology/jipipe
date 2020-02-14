package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQMutableSlotConfiguration;

public class SlotRemovedEvent {
    private ACAQMutableSlotConfiguration slotConfiguration;
    private String slotName;

    public SlotRemovedEvent(ACAQMutableSlotConfiguration slotConfiguration, String slotName) {
        this.slotConfiguration = slotConfiguration;
        this.slotName = slotName;
    }

    public ACAQMutableSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public String getSlotName() {
        return slotName;
    }
}
