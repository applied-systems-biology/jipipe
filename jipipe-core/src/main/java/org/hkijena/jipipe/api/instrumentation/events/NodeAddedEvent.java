package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class NodeAddedEvent extends InstrumentationEvent {
    private final String compartmentId;
    private final String nodeId;
    private final String nodeTypeId;
    private final String nodeName;

    public NodeAddedEvent(String projectId, String compartmentId, String nodeId, String nodeTypeId, String nodeName) {
        super(projectId);
        this.compartmentId = compartmentId;
        this.nodeId = nodeId;
        this.nodeTypeId = nodeTypeId;
        this.nodeName = nodeName;
    }

    public String getCompartmentId() { return compartmentId; }
    public String getNodeId() { return nodeId; }
    public String getNodeTypeId() { return nodeTypeId; }
    public String getNodeName() { return nodeName; }
}
