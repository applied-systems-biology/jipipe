package org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class NodeUIActionRequestedEventEmitter extends JIPipeEventEmitter<NodeUIActionRequestedEvent, NodeUIActionRequestedEventListener> {

    @Override
    protected void call(NodeUIActionRequestedEventListener nodeUIActionRequestedEventListener, NodeUIActionRequestedEvent event) {
        nodeUIActionRequestedEventListener.onNodeUIActionRequested(event);
    }
}
