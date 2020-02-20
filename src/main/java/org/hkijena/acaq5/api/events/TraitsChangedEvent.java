package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.ACAQTraitConfiguration;

public class TraitsChangedEvent {
    private ACAQTraitConfiguration source;

    public TraitsChangedEvent(ACAQTraitConfiguration source) {
        this.source = source;
    }

    public ACAQTraitConfiguration getSource() {
        return source;
    }
}
