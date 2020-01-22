package org.hkijena.acaq5;

public class ACAQOutputDataSlot<T extends ACAQData> extends ACAQDataSlot<T> {
    public ACAQOutputDataSlot(String name, Class<T> acceptedDataType) {
        super(SlotType.Output, name, acceptedDataType);
    }
}
