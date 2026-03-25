package org.hkijena.jipipe.desktop.commons.components.icons;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopDualIcon implements Icon {

    private final Icon left;
    private final Icon right;
    private final int spacing;


    public JIPipeDesktopDualIcon(Icon left, Icon right, int spacing) {
        this.left = left;
        this.right = right;
        this.spacing = spacing;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        left.paintIcon(c, g, x, y);
        right.paintIcon(c, g, x + left.getIconWidth() + spacing, y);
    }

    @Override
    public int getIconWidth() {
        return left.getIconWidth() + spacing + right.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return Math.max(left.getIconHeight(), right.getIconHeight());
    }
}
