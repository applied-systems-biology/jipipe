package org.hkijena.jipipe.ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Icon that adapts to the zoom level of a {@link ZoomViewPort}
 */
public class ZoomIcon implements Icon {

    private final ZoomViewPort viewPort;
    private ImageIcon icon;

    public ZoomIcon(ImageIcon icon, ZoomViewPort viewPort) {
        this.icon = icon;
        this.viewPort = viewPort;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (icon != null) {
            g.drawImage(icon.getImage(), x, y, getIconWidth(), getIconHeight(), null);
        }
    }

    @Override
    public int getIconWidth() {
        if (icon != null) {
            return (int) Math.round(icon.getIconWidth() * viewPort.getZoom());
        }
        return 0;
    }

    @Override
    public int getIconHeight() {
        if (icon != null) {
            return (int) Math.round(icon.getIconHeight() * viewPort.getZoom());
        }
        return 0;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        this.icon = icon;
    }

    public ZoomViewPort getViewPort() {
        return viewPort;
    }
}
