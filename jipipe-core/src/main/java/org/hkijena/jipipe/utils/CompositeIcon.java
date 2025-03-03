package org.hkijena.jipipe.utils;

import javax.swing.*;
import java.awt.*;

public class CompositeIcon implements Icon {

    private final Icon backgroundIcon;
    private final Icon foregroundIcon;
    private final int offsetX;
    private final int offsetY;

    public CompositeIcon(Icon backgroundIcon, Icon foregroundIcon, int offsetX, int offsetY) {
        this.backgroundIcon = backgroundIcon;
        this.foregroundIcon = foregroundIcon;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public int getIconWidth() {
        return backgroundIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return backgroundIcon.getIconHeight();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        backgroundIcon.paintIcon(c, g, x, y);

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            // Draw foreground with transparency support
            foregroundIcon.paintIcon(c, g2, x + offsetX, y + offsetY);
        } finally {
            g2.dispose();
        }
    }
}

