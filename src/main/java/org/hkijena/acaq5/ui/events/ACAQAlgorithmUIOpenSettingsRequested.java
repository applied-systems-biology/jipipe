package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

public class ACAQAlgorithmUIOpenSettingsRequested {
    private ACAQAlgorithmUI ui;

    public ACAQAlgorithmUIOpenSettingsRequested(ACAQAlgorithmUI ui) {
        this.ui = ui;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }
}
