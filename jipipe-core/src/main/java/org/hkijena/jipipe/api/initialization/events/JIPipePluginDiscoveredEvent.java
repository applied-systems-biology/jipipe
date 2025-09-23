package org.hkijena.jipipe.api.initialization.events;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Triggered when a new extension was discovered
 */
public class JIPipePluginDiscoveredEvent extends AbstractJIPipeEvent {
    private final JIPipe registry;
    private final JIPipeDependency extension;

    public JIPipePluginDiscoveredEvent(JIPipe registry, JIPipeDependency extension) {
        super(registry);
        this.registry = registry;
        this.extension = extension;
    }

    public JIPipe getRegistry() {
        return registry;
    }

    public JIPipeDependency getExtension() {
        return extension;
    }
}
