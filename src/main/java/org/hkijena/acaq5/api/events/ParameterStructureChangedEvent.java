package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.parameters.ACAQParameterHolder;

/**
 * Triggered by an {@link ACAQParameterHolder} if the list of available parameters is changed
 */
public class ParameterStructureChangedEvent {
    private ACAQParameterHolder parameterHolder;

    public ParameterStructureChangedEvent(ACAQParameterHolder parameterHolder) {
        this.parameterHolder = parameterHolder;
    }

    public ACAQParameterHolder getParameterHolder() {
        return parameterHolder;
    }
}
