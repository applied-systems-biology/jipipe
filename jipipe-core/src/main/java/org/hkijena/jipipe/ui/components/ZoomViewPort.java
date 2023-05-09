package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public interface ZoomViewPort {
    EventBus getEventBus();

    double getZoom();

    /**
     * Triggered by {@link ZoomViewPort} when the zoom was changed
     */
    class ZoomChangedEvent extends AbstractJIPipeEvent {
        private final ZoomViewPort viewPort;

        public ZoomChangedEvent(ZoomViewPort viewPort) {
            super(viewPort);
            this.viewPort = viewPort;
        }

        public ZoomViewPort getViewPort() {
            return viewPort;
        }
    }

    interface ZoomChangedEventListener {
        void onViewPortZoomChanged(ZoomChangedEvent event);
    }

    class ZoomChangedEventEmitter extends JIPipeEventEmitter<ZoomChangedEvent, ZoomChangedEventListener> {

        @Override
        protected void call(ZoomChangedEventListener zoomChangedEventListener, ZoomChangedEvent event) {
            zoomChangedEventListener.onViewPortZoomChanged(event);
        }
    }
}
