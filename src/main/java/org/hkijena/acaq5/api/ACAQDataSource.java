package org.hkijena.acaq5.api;

/**
 * An {@link ACAQAlgorithm} that generates data. It has no input slots.
 */
public abstract class ACAQDataSource extends ACAQAlgorithm {
    public ACAQDataSource(ACAQMutableSlotConfiguration configuration) {
        super(configuration);
        if(configuration.hasInputSlots())
            throw new IllegalArgumentException("Data sources cannot have input slots!");
    }
}
