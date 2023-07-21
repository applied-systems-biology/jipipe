package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MouseClickedEventEmitter extends JIPipeEventEmitter<MouseClickedEvent, MouseClickedEventListener> {
    @Override
    protected void call(MouseClickedEventListener mouseClickedEventListener, MouseClickedEvent event) {
        mouseClickedEventListener.onComponentMouseClicked(event);
    }
}
