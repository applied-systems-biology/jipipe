package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;

/**
 * Triggered after an {@link ACAQMutableSlotConfiguration} renamed a slot
 */
public class SlotRenamedEvent {
    private ACAQMutableSlotConfiguration slotConfiguration;
    private String oldSlotName;
    private String newSlotName;

    /**
     * @param slotConfiguration the slot configuration
     * @param oldSlotName       old slot name
     * @param newSlotName       new slot name
     */
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
