/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;

public interface ZoomViewPort {

    ZoomChangedEventEmitter getZoomChangedEventEmitter();

    double getZoom();

    interface ZoomChangedEventListener {
        void onViewPortZoomChanged(ZoomChangedEvent event);
    }

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

    class ZoomChangedEventEmitter extends JIPipeEventEmitter<ZoomChangedEvent, ZoomChangedEventListener> {

        @Override
        protected void call(ZoomChangedEventListener zoomChangedEventListener, ZoomChangedEvent event) {
            zoomChangedEventListener.onViewPortZoomChanged(event);
        }
    }
}
