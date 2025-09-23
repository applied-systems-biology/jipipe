package org.hkijena.jipipe.api.service.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class JIPipeDatatypeRegisteredEventEmitter extends JIPipeEventEmitter<JIPipeDatatypeRegisteredEvent, JIPipeDatatypeRegisteredEventListener> {
    @Override
    protected void call(JIPipeDatatypeRegisteredEventListener datatypeRegisteredEventListener, JIPipeDatatypeRegisteredEvent event) {
        datatypeRegisteredEventListener.onJIPipeDatatypeRegistered(event);
    }
}
