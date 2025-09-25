package org.hkijena.jipipe.api.service.events;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.service.JIPipeService;

/**
 * Triggered by {@link JIPipeService} when an extension is registered
 */
public class JIPipePluginRegisteredEvent extends AbstractJIPipeEvent {
    private final JIPipeService registry;
    private final JIPipeDependency extension;

    /**
     * @param registry  event source
     * @param extension registered extension
     */
    public JIPipePluginRegisteredEvent(JIPipeService registry, JIPipeDependency extension) {
        super(registry);
        this.registry = registry;
        this.extension = extension;
    }

    public JIPipeService getRegistry() {
        return registry;
    }

    public JIPipeDependency getExtension() {
        return extension;
    }
}
