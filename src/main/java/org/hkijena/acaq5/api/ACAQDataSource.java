package org.hkijena.acaq5.api;

/**
 * An {@link ACAQAlgorithm} that generates data. It has no input slots.
 */
public abstract class ACAQDataSource<T extends ACAQData> extends ACAQAlgorithm {

    private Class<? extends ACAQData> generatedDataClass;

    public ACAQDataSource(ACAQMutableSlotConfiguration configuration, Class<? extends T> generatedDataClass) {
        super(configuration);
        this.generatedDataClass = generatedDataClass;
        if(configuration.hasInputSlots())
            throw new IllegalArgumentException("Data sources cannot have input slots!");
    }

    public Class<? extends ACAQData> getGeneratedDataClass() {
        return generatedDataClass;
    }
}
