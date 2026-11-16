package org.hkijena.jipipe.api.automation.events;

import org.hkijena.jipipe.api.automation.InstrumentationEvent;

public class NodeRemovedEvent extends InstrumentationEvent {
    private final String nodeId;

    public NodeRemovedEvent(String projectId, String nodeId) {
        super(projectId);
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
