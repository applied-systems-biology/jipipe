package org.hkijena.jipipe.ui.components;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.components.icons.ZoomIcon;

import javax.swing.*;
import java.awt.*;

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
        setBackground(UIManager.getColor("TextArea.background"));
        setOpaque(false);
        viewPort.getEventBus().register(this);
        updateSize();
    }

    public ZoomFlatIconButton(ImageIcon icon, ZoomViewPort viewPort, int originalSize, int originalBorderSize) {
        super(new ZoomIcon(icon, viewPort));
        this.originalSize = originalSize;
        this.originalBorderSize = originalBorderSize;
        this.viewPort = viewPort;
        setBackground(UIManager.getColor("TextArea.background"));
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
    public void onZoomChanged(ZoomViewPort.ZoomChangedEvent event) {
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
