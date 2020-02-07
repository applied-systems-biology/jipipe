package org.hkijena.acaq5.api;

import java.util.Collections;
import java.util.Map;

/**
 * An {@link ACAQAlgorithm} that generates data. It has no input slots.
 */
public abstract class ACAQDataSource extends ACAQAlgorithm {
    public ACAQDataSource(ACAQSlotConfiguration configuration) {
        super(configuration);
        if(configuration.hasInputSlots())
            throw new IllegalArgumentException("Data sources cannot have input slots!");
    }
}
