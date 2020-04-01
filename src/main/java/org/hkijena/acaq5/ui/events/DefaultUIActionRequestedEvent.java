package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

/**
 * Triggered when an {@link ACAQAlgorithmUI} requests a default action (double click)
 */
public class DefaultUIActionRequestedEvent {
    private ACAQAlgorithmUI ui;

    /**
     * @param ui event source
     */
    public DefaultUIActionRequestedEvent(ACAQAlgorithmUI ui) {
        this.ui = ui;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }
}
