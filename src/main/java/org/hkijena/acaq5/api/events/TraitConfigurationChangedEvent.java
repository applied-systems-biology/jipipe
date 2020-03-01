package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.traits.ACAQTraitConfiguration;

/**
 * Triggered after an {@link ACAQTraitConfiguration} was changed
 */
public class TraitConfigurationChangedEvent {
    private ACAQTraitConfiguration source;

    public TraitConfigurationChangedEvent(ACAQTraitConfiguration source) {
        this.source = source;
    }

    public ACAQTraitConfiguration getSource() {
        return source;
    }
}
