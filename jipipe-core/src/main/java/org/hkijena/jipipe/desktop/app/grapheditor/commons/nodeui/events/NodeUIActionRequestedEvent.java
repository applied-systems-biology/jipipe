package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.actions.JIPipeDesktopNodeUIAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

/**
 * An action that is requested by an {@link JIPipeDesktopGraphNodeUI} and passed down to a {@link JIPipeDesktopGraphEditorUI}
 */
public class NodeUIActionRequestedEvent extends AbstractJIPipeEvent {
    private final JIPipeDesktopGraphNodeUI ui;
    private final JIPipeDesktopNodeUIAction action;

    /**
     * Initializes a new instance
     *
     * @param ui     the requesting UI
     * @param action the action parameter
     */
    public NodeUIActionRequestedEvent(JIPipeDesktopGraphNodeUI ui, JIPipeDesktopNodeUIAction action) {
        super(ui);
        this.ui = ui;
        this.action = action;
    }

    public JIPipeDesktopGraphNodeUI getUi() {
        return ui;
    }

    public JIPipeDesktopNodeUIAction getAction() {
        return action;
    }
}
