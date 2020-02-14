package org.hkijena.acaq5.ui.resultanalysis;

import org.hkijena.acaq5.api.ACAQDataSlot;
import org.hkijena.acaq5.ui.ACAQUIPanel;
import org.hkijena.acaq5.ui.ACAQWorkbenchUI;

import javax.swing.*;

public class ACAQResultDataSlotUI<T extends ACAQDataSlot<?>> extends ACAQUIPanel {
    private T slot;

    public ACAQResultDataSlotUI(ACAQWorkbenchUI workbenchUI, T slot) {
        super(workbenchUI);
        this.slot = slot;
    }

    public T getSlot() {
        return slot;
    }
}
