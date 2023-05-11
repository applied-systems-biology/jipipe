package org.hkijena.jipipe.ui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class WindowOpenedEventEmitter extends JIPipeEventEmitter<WindowOpenedEvent, WindowOpenedEventListener> {
    @Override
    protected void call(WindowOpenedEventListener windowOpenedEventListener, WindowOpenedEvent event) {
        windowOpenedEventListener.onWindowOpened(event);
    }
}
