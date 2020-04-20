package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

/**
 * An event around {@link ACAQAlgorithmUI}
 */
public class AlgorithmEvent {
    private ACAQAlgorithmUI ui;

    /**
     * Creates a new event
     *
     * @param ui the algorithm
     */
    public AlgorithmEvent(ACAQAlgorithmUI ui) {
        this.ui = ui;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }
}
