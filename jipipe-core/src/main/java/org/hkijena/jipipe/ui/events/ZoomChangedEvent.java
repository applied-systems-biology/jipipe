package org.hkijena.jipipe.ui.events;

import org.hkijena.jipipe.ui.components.ZoomViewPort;

/**
 * Triggered by {@link org.hkijena.jipipe.ui.components.ZoomViewPort} when the zoom was changed
 */
public class ZoomChangedEvent {
    private final ZoomViewPort viewPort;

    public ZoomChangedEvent(ZoomViewPort viewPort) {
        this.viewPort = viewPort;
    }

    public ZoomViewPort getViewPort() {
        return viewPort;
    }
}
