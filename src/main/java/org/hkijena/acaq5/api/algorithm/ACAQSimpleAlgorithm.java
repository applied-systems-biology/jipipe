package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;

/**
 * A simple input-to-output algorithm
 * @param <IData>
 * @param <OData>
 */
public abstract class ACAQSimpleAlgorithm<IData extends ACAQData, OData extends ACAQData> extends ACAQAlgorithm {

    /**
     * Instantiates the algorithm with autoconfiguration via {@link AlgorithmInputSlot} and {@link AlgorithmOutputSlot}
     */
    public ACAQSimpleAlgorithm() {
        super(null, null);
    }

    public ACAQSimpleAlgorithm(String inputSlotName, Class<? extends ACAQDataSlot<IData>> inputSlotClass,
                               String outputSlotName, Class<? extends ACAQDataSlot<OData>> outputSlotClass) {
        super(ACAQMutableSlotConfiguration.builder()
                .addInputSlot(inputSlotName, inputSlotClass)
                .addOutputSlot(outputSlotName, outputSlotClass)
                .seal()
                .build(), null);
    }

    public ACAQSimpleAlgorithm(ACAQSimpleAlgorithm<IData, OData> other) {
        super(other);
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
