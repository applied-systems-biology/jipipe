package org.hkijena.acaq5.api;

public abstract class ACAQSimpleAlgorithm<I extends ACAQInputDataSlot, O extends ACAQOutputDataSlot> extends ACAQAlgorithm {
    public ACAQSimpleAlgorithm(ACAQInputDataSlot inputSlot, ACAQOutputDataSlot outputSlot) {
        super(inputSlot, outputSlot);
    }

    public I getInputSlot() {
        return (I)getInputSlots().get(getInputSlots().keySet().iterator().next());
    }

    public O getOutputSlot() {
        return (O)getOutputSlots().get(getOutputSlots().keySet().iterator().next());
    }

}
