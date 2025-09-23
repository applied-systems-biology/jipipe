package org.hkijena.jipipe.api.initialization.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class JIPipePluginRegisteredEventEmitter extends JIPipeEventEmitter<JIPipePluginRegisteredEvent, JIPipePluginRegisteredEventListener> {

    @Override
    protected void call(JIPipePluginRegisteredEventListener pluginRegisteredEventListener, JIPipePluginRegisteredEvent event) {
        pluginRegisteredEventListener.onJIPipePluginRegistered(event);
    }
}
