package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.EventBus;

public interface ZoomViewPort {
    EventBus getEventBus();

    double getZoom();
}
