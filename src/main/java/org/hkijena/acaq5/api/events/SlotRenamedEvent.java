package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;

public class SlotRenamedEvent {
    private ACAQMutableSlotConfiguration slotConfiguration;
    private String oldSlotName;
    private String newSlotName;

    public SlotRenamedEvent(ACAQMutableSlotConfiguration slotConfiguration, String oldSlotName, String newSlotName) {
        this.slotConfiguration = slotConfiguration;
        this.oldSlotName = oldSlotName;
        this.newSlotName = newSlotName;
    }

    public ACAQMutableSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    public String getOldSlotName() {
        return oldSlotName;
    }

    public String getNewSlotName() {
        return newSlotName;
    }
}
