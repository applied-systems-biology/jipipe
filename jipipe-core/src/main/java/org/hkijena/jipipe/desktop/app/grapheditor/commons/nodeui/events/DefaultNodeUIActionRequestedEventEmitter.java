package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class DefaultNodeUIActionRequestedEventEmitter extends JIPipeEventEmitter<DefaultNodeUIActionRequestedEvent, DefaultNodeUIActionRequestedEventListener> {

    @Override
    protected void call(DefaultNodeUIActionRequestedEventListener defaultNodeUIActionRequestedEventListener, DefaultNodeUIActionRequestedEvent event) {
        defaultNodeUIActionRequestedEventListener.onDefaultNodeUIActionRequested(event);
    }
}
