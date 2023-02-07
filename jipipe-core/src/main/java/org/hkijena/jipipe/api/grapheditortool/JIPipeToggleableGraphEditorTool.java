package org.hkijena.jipipe.api.grapheditortool;

public interface JIPipeToggleableGraphEditorTool extends JIPipeGraphEditorTool {

    void deactivate();

    default boolean allowsDragNodes() {
        return true;
    }

    default boolean allowsDragConnections() {
        return true;
    }
    
}
