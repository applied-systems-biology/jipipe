package org.hkijena.jipipe.ui.algorithmfinder;

import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public class AlgorithmFinderSuccessEventEmitter extends JIPipeEventEmitter<AlgorithmFinderSuccessEvent, AlgorithmFinderSuccessEventListener> {
    @Override
    protected void call(AlgorithmFinderSuccessEventListener algorithmFinderSuccessEventListener, AlgorithmFinderSuccessEvent event) {
        algorithmFinderSuccessEventListener.onAlgorithmFinderSuccess(event);
    }
}
