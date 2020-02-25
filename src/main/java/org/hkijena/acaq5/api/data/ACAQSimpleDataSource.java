package org.hkijena.acaq5.api.data;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;

/**
 * A helper that simplifies the creation of an {@link ACAQDataSource} algorithm
 * @param <T>
 */
public abstract class ACAQSimpleDataSource<T extends ACAQData> extends ACAQDataSource<T> {

    public ACAQSimpleDataSource(String name, ACAQAlgorithmDeclaration declaration, Class<? extends ACAQDataSlot<T>> slotClass) {
        super(declaration, ACAQMutableSlotConfiguration.builder().addOutputSlot(name, slotClass).seal().build());
    }

    public ACAQSimpleDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
    }

    public ACAQSimpleDataSource(ACAQSimpleDataSource<T> other) {
        super(other);
    }

    public ACAQDataSlot<T> getOutputSlot() {
        return (ACAQDataSlot<T>)getSlots().values().iterator().next();
    }

    public void setOutputData(T data) {
        getOutputSlot().setData(data);
    }
}
