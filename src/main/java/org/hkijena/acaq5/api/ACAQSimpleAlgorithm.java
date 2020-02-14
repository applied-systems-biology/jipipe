package org.hkijena.acaq5.api;

public abstract class ACAQSimpleAlgorithm<IData extends ACAQData, OData extends ACAQData> extends ACAQAlgorithm {
    public ACAQSimpleAlgorithm(String inputSlotName, Class<? extends ACAQDataSlot<IData>> inputSlotClass,
                               String outputSlotName, Class<? extends ACAQDataSlot<OData>> outputSlotClass) {
        super(ACAQMutableSlotConfiguration.builder()
                .addInputSlot(inputSlotName, inputSlotClass)
                .addOutputSlot(outputSlotName, outputSlotClass)
                .seal()
                .build());
    }

    public ACAQDataSlot<IData> getInputSlot() {
        return (ACAQDataSlot<IData>) getInputSlots().get(0);
    }

    public ACAQDataSlot<OData> getOutputSlot() {
        return (ACAQDataSlot<OData>) getOutputSlots().get(0);
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
