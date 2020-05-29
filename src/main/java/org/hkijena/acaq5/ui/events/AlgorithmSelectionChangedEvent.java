package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI;

/**
 * Triggered when An {@link org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphCanvasUI} selection was changed
 */
public class AlgorithmSelectionChangedEvent {
    private ACAQAlgorithmGraphCanvasUI canvasUI;

    /**
     * @param canvasUI the canvas that triggered the event
     */
    public AlgorithmSelectionChangedEvent(ACAQAlgorithmGraphCanvasUI canvasUI) {

        this.canvasUI = canvasUI;
    }

    public ACAQAlgorithmGraphCanvasUI getCanvasUI() {
        return canvasUI;
    }
}
