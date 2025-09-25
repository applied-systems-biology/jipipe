package org.hkijena.jipipe.api.service.events;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.service.JIPipeService;

/**
 * Triggered when a new data type is registered
 */
public class JIPipeDatatypeRegisteredEvent extends AbstractJIPipeEvent {
    private final JIPipeService service;
    private final String id;

    /**
     * @param service the event source
     * @param id      the data type id
     */
    public JIPipeDatatypeRegisteredEvent(JIPipeService service, String id) {
        super(service);
        this.service = service;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public JIPipeService getService() {
        return service;
    }
}
