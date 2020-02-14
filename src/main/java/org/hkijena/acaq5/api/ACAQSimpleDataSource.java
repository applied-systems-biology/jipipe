package org.hkijena.acaq5.api;

public abstract class ACAQSimpleDataSource<T extends ACAQData> extends ACAQDataSource<T> {

    public ACAQSimpleDataSource(String name, Class<? extends ACAQDataSlot<T>> slotClass, Class<? extends T> generatedDataClass) {
        super(ACAQMutableSlotConfiguration.builder().addOutputSlot(name, slotClass).seal().build(), generatedDataClass);
    }

    public ACAQDataSlot<T> getOutputSlot() {
        return (ACAQDataSlot<T>)getSlots().values().iterator().next();
    }

    public void setOutputData(T data) {
        getOutputSlot().setData(data);
    }
}
