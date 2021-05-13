package org.hkijena.jipipe.extensions.r;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;

public class REnvironmentSettings implements ExternalEnvironmentSettings<REnvironment, REnvironment.List> {
    private final EventBus eventBus = new EventBus();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
