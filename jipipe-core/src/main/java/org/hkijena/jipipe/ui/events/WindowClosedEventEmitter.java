package org.hkijena.jipipe.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class WindowClosedEventEmitter extends JIPipeEventEmitter<WindowClosedEvent, WindowClosedEventListener> {
    @Override
    protected void call(WindowClosedEventListener windowClosedEventListener, WindowClosedEvent event) {
        windowClosedEventListener.onWindowClosed(event);
    }
}
