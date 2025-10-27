package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopAnnotationGraphNodeUI;

/**
 * Used by the {@link org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI} to filter out events
 * sent to interactable objects
 */
public enum JIPipeDesktopToggleableGraphEditorToolNodeLayerMask {
    WorkflowOnly,
    AnnotationsOnly,
    None;

    public boolean test(JIPipeDesktopGraphInteractiveObjectUI ui) {
        if (this == WorkflowOnly) {
            if (ui instanceof JIPipeDesktopAnnotationGraphNodeUI) {
                return !(ui instanceof JIPipeDesktopAnnotationGraphNodeUI);
            }
            return true;
        } else if (this == AnnotationsOnly) {
            return (ui instanceof JIPipeDesktopAnnotationGraphNodeUI);
        } else {
            return true;
        }
    }
}
