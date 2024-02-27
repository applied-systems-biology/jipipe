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

package org.hkijena.jipipe.ui.components.icons;

import org.hkijena.jipipe.ui.components.ZoomViewPort;

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
