package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

/**
 * Triggered by an {@link ACAQParameterCollection} if the list of available parameters is changed
 */
public class ParameterStructureChangedEvent {
    private ACAQParameterCollection parameterHolder;

    /**
     * @param parameterHolder event source
     */
    public ParameterStructureChangedEvent(ACAQParameterCollection parameterHolder) {
        this.parameterHolder = parameterHolder;
    }

    public ACAQParameterCollection getParameterHolder() {
        return parameterHolder;
    }
}
