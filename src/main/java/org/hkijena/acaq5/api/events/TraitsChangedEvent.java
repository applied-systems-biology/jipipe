package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.traits.global.ACAQTraitConfiguration;

/**
 * Triggered after an {@link ACAQTraitConfiguration} was changed
 */
public class TraitsChangedEvent {
    private ACAQTraitConfiguration source;

    public TraitsChangedEvent(ACAQTraitConfiguration source) {
        this.source = source;
    }

    public ACAQTraitConfiguration getSource() {
        return source;
    }
}
