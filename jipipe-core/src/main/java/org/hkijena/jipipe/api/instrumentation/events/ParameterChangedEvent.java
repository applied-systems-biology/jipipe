package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ParameterChangedEvent extends InstrumentationEvent {
    private final String nodeId;
    private final String key;
    private final String value;

    public ParameterChangedEvent(String projectId, String nodeId, String key, String value) {
        super(projectId);
        this.nodeId = nodeId;
        this.key = key;
        this.value = value;
    }

    public String getNodeId() { return nodeId; }
    public String getKey() { return key; }
    public String getValue() { return value; }
}
