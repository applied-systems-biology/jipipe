package org.hkijena.jipipe.extensions.nodetemplate;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class NodeTemplatesRefreshedEventEmitter extends JIPipeEventEmitter<NodeTemplatesRefreshedEvent, NodeTemplatesRefreshedEventListener> {
    @Override
    protected void call(NodeTemplatesRefreshedEventListener nodeTemplatesRefreshedEventListener, NodeTemplatesRefreshedEvent event) {
        nodeTemplatesRefreshedEventListener.onNodeTemplatesRefreshed(event);
    }
}
