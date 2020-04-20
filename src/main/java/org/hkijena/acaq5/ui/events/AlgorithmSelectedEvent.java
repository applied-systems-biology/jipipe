package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

/**
 * Generated when an algorithm is selected
 */
public class AlgorithmSelectedEvent extends AlgorithmEvent {
    private boolean addToSelection;

    /**
     * @param ui             the algorithm UI
     * @param addToSelection if the algorithm should be added to the selection
     */
    public AlgorithmSelectedEvent(ACAQAlgorithmUI ui, boolean addToSelection) {
        super(ui);
        this.addToSelection = addToSelection;
    }

    public boolean isAddToSelection() {
        return addToSelection;
    }
}
