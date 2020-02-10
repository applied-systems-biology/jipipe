package org.hkijena.acaq5.api;

public abstract class ACAQSimpleAlgorithm<IData extends ACAQData, OData extends ACAQData> extends ACAQAlgorithm {
    public ACAQSimpleAlgorithm(String inputSlotName, Class<? extends ACAQDataSlot<IData>> inputSlotClass,
                               String outputSlotName, Class<? extends ACAQDataSlot<OData>> outputSlotClass) {
        super(new ACAQMutableSlotConfiguration()
                .addInputSlot(inputSlotName, inputSlotClass)
                .addOutputSlot(outputSlotName, outputSlotClass));
    }

    public ACAQDataSlot<IData> getInputSlot() {
        return (ACAQDataSlot<IData>)getInputSlots().get(getInputSlots().keySet().iterator().next());
    }

    public ACAQDataSlot<OData> getOutputSlot() {
        return (ACAQDataSlot<OData>)getOutputSlots().get(getOutputSlots().keySet().iterator().next());
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
