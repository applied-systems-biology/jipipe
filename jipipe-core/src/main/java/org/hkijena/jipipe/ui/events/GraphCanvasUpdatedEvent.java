package org.hkijena.jipipe.ui.events;

import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;

/**
 * Triggered when a graph canvas was updated
 */
public class GraphCanvasUpdatedEvent {
    private final JIPipeGraphCanvasUI graphCanvasUI;

    public GraphCanvasUpdatedEvent(JIPipeGraphCanvasUI graphCanvasUI) {
        this.graphCanvasUI = graphCanvasUI;
    }

    public JIPipeGraphCanvasUI getGraphCanvasUI() {
        return graphCanvasUI;
    }
}
