package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;

public class ReloadSettingsRequestedEvent {
    private ACAQAlgorithm algorithm;

    public ReloadSettingsRequestedEvent(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }
}
