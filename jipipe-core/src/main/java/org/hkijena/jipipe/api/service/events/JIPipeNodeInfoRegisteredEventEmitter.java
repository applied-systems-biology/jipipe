package org.hkijena.jipipe.api.service.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class JIPipeNodeInfoRegisteredEventEmitter extends JIPipeEventEmitter<JIPipeNodeInfoRegisteredEvent, JIPipeNodeInfoRegisteredEventListener> {

    @Override
    protected void call(JIPipeNodeInfoRegisteredEventListener nodeInfoRegisteredEventListener, JIPipeNodeInfoRegisteredEvent event) {
        nodeInfoRegisteredEventListener.onJIPipeNodeInfoRegistered(event);
    }
}
