package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.data.ACAQDataSlot;

public class AlgorithmFinderSuccessEvent {
    private ACAQDataSlot outputSlot;
    private ACAQDataSlot inputSlot;

    public AlgorithmFinderSuccessEvent(ACAQDataSlot outputSlot, ACAQDataSlot inputSlot) {
        this.outputSlot = outputSlot;
        this.inputSlot = inputSlot;
    }

    public ACAQDataSlot getOutputSlot() {
        return outputSlot;
    }

    public ACAQDataSlot getInputSlot() {
        return inputSlot;
    }
}
