package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class ConnectionChangedEvent extends InstrumentationEvent {
    private final String source;
    private final String sourceSlot;
    private final String target;
    private final String targetSlot;
    private final String action;

    public ConnectionChangedEvent(String projectId, String source, String sourceSlot, String target, String targetSlot, String action) {
        super(projectId);
        this.source = source;
        this.sourceSlot = sourceSlot;
        this.target = target;
        this.targetSlot = targetSlot;
        this.action = action;
    }

    public String getSource() { return source; }
    public String getSourceSlot() { return sourceSlot; }
    public String getTarget() { return target; }
    public String getTargetSlot() { return targetSlot; }
    public String getAction() { return action; }
}
