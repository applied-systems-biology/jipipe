package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.data.ACAQDataSlot;

/**
 * Generated when the algorithm finder successfully found a connection
 */
public class AlgorithmFinderSuccessEvent {
    private ACAQDataSlot outputSlot;
    private ACAQDataSlot inputSlot;

    /**
     * @param outputSlot The output slot
     * @param inputSlot  The target slot
     */
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
