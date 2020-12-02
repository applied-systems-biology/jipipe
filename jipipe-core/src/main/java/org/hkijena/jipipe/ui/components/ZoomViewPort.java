package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.EventBus;

public interface ZoomViewPort {
    EventBus getEventBus();

    double getZoom();

    /**
     * Triggered by {@link ZoomViewPort} when the zoom was changed
     */
    class ZoomChangedEvent {
        private final ZoomViewPort viewPort;

        public ZoomChangedEvent(ZoomViewPort viewPort) {
            this.viewPort = viewPort;
        }

        public ZoomViewPort getViewPort() {
            return viewPort;
        }
    }
}
