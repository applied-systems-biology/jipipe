package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.events.ZoomChangedEvent;

import javax.swing.*;
import java.awt.Color;
import java.awt.Dimension;

/**
 * Flat {@link javax.swing.JButton} that zooms and displays a zoomable icon
 */
public class ZoomFlatIconButton extends JButton {

    private final ZoomViewPort viewPort;
    private int originalSize = 25;
    private int originalBorderSize = 3;

    public ZoomFlatIconButton(ImageIcon icon, ZoomViewPort viewPort) {
        super(new ZoomIcon(icon, viewPort));
        this.viewPort = viewPort;
        setBackground(Color.WHITE);
        setOpaque(false);
        viewPort.getEventBus().register(this);
        updateSize();
    }

    public void updateSize() {
        int size = Math.max(1, (int) Math.round(viewPort.getZoom() * originalSize));
        int borderSize = Math.max(1, (int) Math.round(viewPort.getZoom() * originalBorderSize));
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setBorder(BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize));
    }

    @Subscribe
    public void onZoomChanged(ZoomChangedEvent event) {
        updateSize();
    }

    public ZoomViewPort getViewPort() {
        return viewPort;
    }

    public int getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(int originalSize) {
        this.originalSize = originalSize;
        updateSize();
    }

    public int getOriginalBorderSize() {
        return originalBorderSize;
    }

    public void setOriginalBorderSize(int originalBorderSize) {
        this.originalBorderSize = originalBorderSize;
        updateSize();
    }
}
