package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQSlotConfiguration;

public class SlotRenamedEvent {
    private ACAQSlotConfiguration slotConfiguration;
    private String oldSlotName;
    private String newSlotName;

    public SlotRenamedEvent(ACAQSlotConfiguration slotConfiguration, String oldSlotName, String newSlotName) {
        this.slotConfiguration = slotConfiguration;
        this.oldSlotName = oldSlotName;
        this.newSlotName = newSlotName;
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public String getOldSlotName() {
        return oldSlotName;
    }

    public String getNewSlotName() {
        return newSlotName;
    }
}
