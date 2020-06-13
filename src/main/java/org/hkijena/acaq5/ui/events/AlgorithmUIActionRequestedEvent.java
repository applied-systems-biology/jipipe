package org.hkijena.acaq5.ui.events;

import org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI;

/**
 * An action that is requested by an {@link ACAQAlgorithmUI} and passed down to a {@link org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmGraphEditorUI}
 */
public class AlgorithmUIActionRequestedEvent {
    private final ACAQAlgorithmUI ui;
    private final Object action;

    /**
     * Initializes a new instance
     *
     * @param ui     the requesting UI
     * @param action the action parameter
     */
    public AlgorithmUIActionRequestedEvent(ACAQAlgorithmUI ui, Object action) {
        this.ui = ui;
        this.action = action;
    }

    public ACAQAlgorithmUI getUi() {
        return ui;
    }

    public Object getAction() {
        return action;
    }
}
