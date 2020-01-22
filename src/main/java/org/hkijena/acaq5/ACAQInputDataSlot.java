package org.hkijena.acaq5;

public class ACAQInputDataSlot<T extends ACAQData> extends ACAQDataSlot<T> {
    public ACAQInputDataSlot(String name, Class<T> acceptedDataType) {
        super(SlotType.Input, name, acceptedDataType);
    }
}
