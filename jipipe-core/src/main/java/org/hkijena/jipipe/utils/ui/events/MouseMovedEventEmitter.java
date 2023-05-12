package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MouseMovedEventEmitter extends JIPipeEventEmitter<MouseMovedEvent, MouseMovedEventListener> {
    @Override
    protected void call(MouseMovedEventListener mouseMovedEventListener, MouseMovedEvent event) {
        mouseMovedEventListener.onComponentMouseMoved(event);
    }
}
