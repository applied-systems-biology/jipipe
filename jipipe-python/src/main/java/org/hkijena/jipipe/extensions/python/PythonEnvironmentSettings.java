package org.hkijena.jipipe.extensions.python;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;

public class PythonEnvironmentSettings implements ExternalEnvironmentSettings<PythonEnvironment, PythonEnvironment.List> {
    private final EventBus eventBus = new EventBus();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
