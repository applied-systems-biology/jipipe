package org.hkijena.acaq5.api;

public abstract class ACAQSimpleAlgorithm<IData extends ACAQData, OData extends ACAQData> extends ACAQAlgorithm {
    public ACAQSimpleAlgorithm(ACAQInputDataSlot<IData> inputSlot, ACAQOutputDataSlot<OData> outputSlot) {
        super(inputSlot, outputSlot);
    }

    public ACAQInputDataSlot<IData> getInputSlot() {
        return (ACAQInputDataSlot<IData>)getInputSlots().get(getInputSlots().keySet().iterator().next());
    }

    public ACAQOutputDataSlot<OData> getOutputSlot() {
        return (ACAQOutputDataSlot<OData>)getOutputSlots().get(getOutputSlots().keySet().iterator().next());
    }

    public IData getInputData() {
        return getInputSlot().getData();
    }

    public void setInputData(IData data) {
        getInputSlot().setData(data);
    }

    public OData getOutputData() {
        return getOutputSlot().getData();
    }

    public void setOutputData(OData data) {
        getOutputSlot().setData(data);
    }
}
