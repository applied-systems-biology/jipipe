package org.hkijena.jipipe.api.service.events;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.service.JIPipeService;

/**
 * Triggered when a new extension was discovered
 */
public class JIPipePluginDiscoveredEvent extends AbstractJIPipeEvent {
    private final JIPipeDependency extension;
    private final JIPipeService service;

    public JIPipePluginDiscoveredEvent(JIPipeService service, JIPipeDependency extension) {
        super(service);
        this.service = service;
        this.extension = extension;
    }

    public JIPipeDependency getExtension() {
        return extension;
    }

    public JIPipeService getService() {
        return service;
    }
}
