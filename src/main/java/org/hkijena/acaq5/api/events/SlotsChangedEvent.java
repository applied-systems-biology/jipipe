package org.hkijena.acaq5.api.events;

import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;

/**
 * Triggered when a {@link org.hkijena.acaq5.api.data.ACAQSlotConfiguration} was changed
 */
public class SlotsChangedEvent {
    private final ACAQSlotConfiguration configuration;

    /**
     * Creates a new instance
     * @param configuration the configuration
     */
    public SlotsChangedEvent(ACAQSlotConfiguration configuration) {
        this.configuration = configuration;
    }

    public ACAQSlotConfiguration getConfiguration() {
        return configuration;
    }
}
