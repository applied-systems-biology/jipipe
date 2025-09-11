package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

/**
 * Triggered when an {@link JIPipeDesktopGraphNodeUI} requests a default action (double click)
 */
public class DefaultNodeUIActionRequestedEvent extends AbstractJIPipeEvent {

    private final JIPipeDesktopGraphNodeUI ui;

    /**
     * @param ui event source
     */
    public DefaultNodeUIActionRequestedEvent(JIPipeDesktopGraphNodeUI ui) {
        super(ui);
        this.ui = ui;
    }

    public JIPipeDesktopGraphNodeUI getUi() {
        return ui;
    }
}
