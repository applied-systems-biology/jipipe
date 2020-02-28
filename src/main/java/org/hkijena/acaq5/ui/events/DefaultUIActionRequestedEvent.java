package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

public class DefaultUIActionRequestedEvent {
    private ACAQAlgorithmUI ui;

    public DefaultUIActionRequestedEvent(ACAQAlgorithmUI ui) {
        this.ui = ui;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }
}
