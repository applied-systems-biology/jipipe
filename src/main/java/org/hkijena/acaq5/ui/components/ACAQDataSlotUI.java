package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;

public class ACAQDataSlotUI extends JButton {
    private ACAQDataSlot<?> slot;

    public ACAQDataSlotUI(ACAQDataSlot<?> slot) {
        super(UIUtils.getIconFromResources("chevron-right.png"));
        this.slot = slot;
    }

    public ACAQDataSlot<?> getSlot() {
        return slot;
    }
}
