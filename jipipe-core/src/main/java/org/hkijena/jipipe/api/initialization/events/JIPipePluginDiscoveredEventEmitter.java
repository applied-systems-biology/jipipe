package org.hkijena.jipipe.api.initialization.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class JIPipePluginDiscoveredEventEmitter extends JIPipeEventEmitter<JIPipePluginDiscoveredEvent, JIPipePluginDiscoveredEventListener> {

    @Override
    protected void call(JIPipePluginDiscoveredEventListener pluginDiscoveredEventListener, JIPipePluginDiscoveredEvent event) {
        pluginDiscoveredEventListener.onJIPipePluginDiscovered(event);
    }
}
