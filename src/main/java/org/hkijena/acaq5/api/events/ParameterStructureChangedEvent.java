package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;

/**
 * Triggered by an {@link ACAQParameterCollection} if the list of available parameters is changed
 */
public class ParameterStructureChangedEvent {
    private ACAQParameterCollection source;

    /**
     * @param source event source
     */
    public ParameterStructureChangedEvent(ACAQParameterCollection source) {
        this.source = source;
    }

    public ACAQParameterCollection getSource() {
        return source;
    }
}
