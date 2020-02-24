package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;

/**
 * Triggered after an {@link ACAQMutableSlotConfiguration} added a slot
 */
public class SlotAddedEvent {
    private ACAQMutableSlotConfiguration slotConfiguration;
    private String slotName;

    public SlotAddedEvent(ACAQMutableSlotConfiguration slotConfiguration, String slotName) {
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
