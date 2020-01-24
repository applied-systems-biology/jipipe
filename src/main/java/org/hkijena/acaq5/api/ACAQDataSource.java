package org.hkijena.acaq5.api;

import java.util.Collections;
import java.util.Map;

/**
 * An {@link ACAQAlgorithm} that generates data. It has no input slots
 */
public abstract class ACAQDataSource<T extends ACAQData> extends ACAQAlgorithm {

    @Override
    public Map<String, ACAQDataSlot> getInputSlots() {
        return Collections.emptyMap();
    }
}
