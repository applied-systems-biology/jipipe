package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MouseExitedEventEmitter extends JIPipeEventEmitter<MouseExitedEvent, MouseExitedEventListener> {
    @Override
    protected void call(MouseExitedEventListener mouseExitedEventListener, MouseExitedEvent event) {
        mouseExitedEventListener.onComponentMouseExited(event);
    }
}
