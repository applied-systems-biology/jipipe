package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

public class AlgorithmSelectedEvent {
    private ACAQAlgorithmUI ui;
    private boolean addToSelection;

    public AlgorithmSelectedEvent(ACAQAlgorithmUI ui, boolean addToSelection) {
        this.ui = ui;
        this.addToSelection = addToSelection;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }

    public boolean isAddToSelection() {
        return addToSelection;
    }
}
