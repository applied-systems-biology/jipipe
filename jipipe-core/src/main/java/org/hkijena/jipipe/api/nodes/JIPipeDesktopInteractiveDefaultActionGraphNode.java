package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events.DefaultNodeUIActionRequestedEvent;

/**
 * A {@link JIPipeGraphNode} that has custom implementations for double-clicking it
 */
public interface JIPipeDesktopInteractiveDefaultActionGraphNode {
    void onDefaultNodeUIActionRequested(JIPipeDesktopGraphEditorUI graphEditorUI, DefaultNodeUIActionRequestedEvent event);
}
