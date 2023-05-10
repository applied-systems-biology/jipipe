package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MouseReleasedEventEmitter extends JIPipeEventEmitter<MouseReleasedEvent, MouseReleasedEventListener> {
    @Override
    protected void call(MouseReleasedEventListener mouseReleasedEventListener, MouseReleasedEvent event) {
        mouseReleasedEventListener.onComponentMouseReleased(event);
    }
}
