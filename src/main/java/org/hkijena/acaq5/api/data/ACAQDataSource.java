package org.hkijena.acaq5.api.data;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitGenerator;

/**
 * An {@link ACAQAlgorithm} that generates data. It has no input slots.
 */
public abstract class ACAQDataSource<T extends ACAQData> extends ACAQAlgorithm {

    public ACAQDataSource(ACAQAlgorithmDeclaration declaration, ACAQMutableSlotConfiguration configuration) {
        super(declaration, configuration, new ACAQMutableTraitGenerator(configuration));
        if(configuration.hasInputSlots())
            throw new IllegalArgumentException("Data sources cannot have input slots!");
    }

    public ACAQDataSource(ACAQAlgorithmDeclaration declaration) {
        super(declaration, null, null);
        if(getSlotConfiguration().getInputSlots().size() > 0)
            throw new IllegalArgumentException("Data sources cannot have input slots!");
    }

    public ACAQDataSource(ACAQDataSource<T> other) {
        super(other);
    }
}
