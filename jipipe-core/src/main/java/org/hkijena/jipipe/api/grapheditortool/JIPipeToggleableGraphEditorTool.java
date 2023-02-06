package org.hkijena.jipipe.api.grapheditortool;

public interface JIPipeToggleableGraphEditorTool extends JIPipeGraphEditorTool {

    void deactivate();

    boolean allowsDragNodes();

    boolean allowsDragConnections();

}
