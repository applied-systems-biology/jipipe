package org.hkijena.jipipe.api.grouping.events;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class ParameterReferencesChangedEventEmitter extends JIPipeEventEmitter<ParameterReferencesChangedEvent, ParameterReferencesChangedEventListener> {
    @Override
    protected void call(ParameterReferencesChangedEventListener parameterReferencesChangedEventListener, ParameterReferencesChangedEvent event) {
        parameterReferencesChangedEventListener.onParameterReferencesChanged(event);
    }
}
