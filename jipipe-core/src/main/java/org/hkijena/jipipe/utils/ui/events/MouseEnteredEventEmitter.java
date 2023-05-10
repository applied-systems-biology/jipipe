package org.hkijena.jipipe.utils.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class MouseEnteredEventEmitter extends JIPipeEventEmitter<MouseEnteredEvent, MouseEnteredEventListener> {
    @Override
    protected void call(MouseEnteredEventListener mouseEnteredEventListener, MouseEnteredEvent event) {
        mouseEnteredEventListener.onComponentMouseEntered(event);
    }
}
