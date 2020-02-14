package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQDataSlot;

import javax.swing.*;

public class ACAQResultDataSlotUI<T extends ACAQDataSlot<?>> extends JPanel {
    private T slot;

    public ACAQResultDataSlotUI(T slot) {
        this.slot = slot;
    }

    public T getSlot() {
        return slot;
    }
}
