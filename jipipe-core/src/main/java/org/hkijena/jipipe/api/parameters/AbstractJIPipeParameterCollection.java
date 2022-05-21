package org.hkijena.jipipe.api.parameters;

import com.google.common.eventbus.EventBus;

/**
 * {@link JIPipeParameterCollection} that implements the {@link com.google.common.eventbus.EventBus}
 */
public class AbstractJIPipeParameterCollection implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
