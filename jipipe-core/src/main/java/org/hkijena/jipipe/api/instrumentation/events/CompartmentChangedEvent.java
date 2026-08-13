package org.hkijena.jipipe.api.instrumentation.events;

import org.hkijena.jipipe.api.instrumentation.InstrumentationEvent;

public class CompartmentChangedEvent extends InstrumentationEvent {
    private final String compartmentId;
    private final String name;
    private final String action;

    public CompartmentChangedEvent(String projectId, String compartmentId, String name, String action) {
        super(projectId);
        this.compartmentId = compartmentId;
        this.name = name;
        this.action = action;
    }

    public String getCompartmentId() { return compartmentId; }
    public String getName() { return name; }
    public String getAction() { return action; }
}
