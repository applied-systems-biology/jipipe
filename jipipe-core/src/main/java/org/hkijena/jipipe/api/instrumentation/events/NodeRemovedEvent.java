package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class NodeRemovedEvent extends InstrumentationEvent {
    private final String nodeId;

    public NodeRemovedEvent(String projectId, String nodeId) {
        super(projectId);
        this.nodeId = nodeId;
    }

    public String getNodeId() { return nodeId; }
}
