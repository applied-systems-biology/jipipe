package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphEdge;

import java.awt.*;

public interface JIPipeToggleableGraphEditorTool extends JIPipeGraphEditorTool {

    void deactivate();

    default boolean allowsDragNodes() {
        return true;
    }

    default boolean allowsDragConnections() {
        return true;
    }

    default boolean canRenderEdge(JIPipeDataSlot source, JIPipeDataSlot target, JIPipeGraphEdge edge) {
        return true;
    }

    default void paintBelowNodesAfterEdges(Graphics2D g) {

    }

    default void paintBelowNodesAndEdges(Graphics2D graphics2D) {

    }

    default void paintAfterNodesAndEdges(Graphics2D graphics2D) {

    }
}
