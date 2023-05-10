package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MousePressedEventEmitter extends JIPipeEventEmitter<MousePressedEvent, MousePressedEventListener> {
    @Override
    protected void call(MousePressedEventListener mousePressedEventListener, MousePressedEvent event) {
        mousePressedEventListener.onComponentMousePressed(event);
    }
}
