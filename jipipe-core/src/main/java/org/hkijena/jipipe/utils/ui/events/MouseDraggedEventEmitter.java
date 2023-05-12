package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MouseDraggedEventEmitter extends JIPipeEventEmitter<MouseDraggedEvent, MouseDraggedEventListener> {
    @Override
    protected void call(MouseDraggedEventListener mouseDraggedEventListener, MouseDraggedEvent event) {
        mouseDraggedEventListener.onComponentMouseDragged(event);
    }
}
