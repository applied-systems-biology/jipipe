package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

public class OpenSettingsUIRequestedEvent {
    private ACAQAlgorithmUI ui;

    public OpenSettingsUIRequestedEvent(ACAQAlgorithmUI ui) {
        this.ui = ui;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }
}
