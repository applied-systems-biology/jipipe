package org.hkijena.jipipe.api.initialization.events;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Triggered when a new data type is registered
 */
public class JIPipeDatatypeRegisteredEvent extends AbstractJIPipeEvent {
    private final JIPipe registry;
    private final String id;

    /**
     * @param registry the event source
     * @param id       the data type id
     */
    public JIPipeDatatypeRegisteredEvent(JIPipe registry, String id) {
        super(registry);
        this.registry = registry;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public JIPipe getRegistry() {
        return registry;
    }
}
