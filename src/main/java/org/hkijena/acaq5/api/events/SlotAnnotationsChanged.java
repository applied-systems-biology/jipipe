package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.ACAQDataSlot;

/**
 * Generated when a slot's annotations are changed
 */
public class SlotAnnotationsChanged {
    private ACAQDataSlot slot;

    /**
     * @param slot the slot
     */
    public SlotAnnotationsChanged(ACAQDataSlot slot) {
        this.slot = slot;
    }

    public ACAQDataSlot getSlot() {
        return slot;
    }
}
