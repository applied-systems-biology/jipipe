package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.events.ZoomChangedEvent;

import javax.swing.*;
import java.awt.*;

/**
 * {@link JLabel} that changes its font based on the {@link ZoomViewPort} zoom level
 */
public class ZoomLabel extends JLabel {
    private final ZoomViewPort viewPort;

    public ZoomLabel(String text, Icon icon, ZoomViewPort viewPort) {
        super(text, icon, JLabel.LEFT);
        this.viewPort = viewPort;
        updateFont();
        viewPort.getEventBus().register(this);
    }

    public ZoomViewPort getViewPort() {
        return viewPort;
    }

    public void updateFont() {
        int fontSize = Math.max(1, (int) Math.round(viewPort.getZoom() * 12));
        setFont(new Font(Font.DIALOG, Font.PLAIN, fontSize));
    }

    @Subscribe
    public void onZoomChanged(ZoomChangedEvent event) {
        updateFont();
    }
}
