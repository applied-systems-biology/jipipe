package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class SlotAnnotationsChanged {
    private ACAQDataSlot slot;

    public SlotAnnotationsChanged(ACAQDataSlot slot) {
        this.slot = slot;
    }

    public ACAQDataSlot getSlot() {
        return slot;
    }
}
