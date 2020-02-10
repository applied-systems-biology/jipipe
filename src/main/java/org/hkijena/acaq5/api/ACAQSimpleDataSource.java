package org.hkijena.acaq5.api;

public abstract class ACAQSimpleDataSource extends ACAQDataSource {
    public ACAQSimpleDataSource(String name, Class<? extends ACAQDataSlot<?>> slotClass) {
        super(new ACAQMutableSlotConfiguration().addOutputSlot(name, slotClass));
    }
}
