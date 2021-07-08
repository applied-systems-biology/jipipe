package org.hkijena.jipipe.utils;

import org.hkijena.jipipe.ui.theme.ModernMetalTheme;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serializable;

/**
 * Taken from {@link javax.swing.plaf.metal.MetalIconFactory}
 */
public class CheckBoxMenuItemIcon implements Icon, UIResource, Serializable {

    private final Color backgroundInactive;

    public CheckBoxMenuItemIcon(Color backgroundInactive) {
        this.backgroundInactive = backgroundInactive;
    }

    protected int getControlSize() {
        return 16;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        ButtonModel model = ((JCheckBoxMenuItem) c).getModel();
        int controlSize = getControlSize();
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (model.isEnabled()) {
            if (model.isSelected()) {
                g.setColor(ModernMetalTheme.PRIMARY5);
                g.fillRoundRect(x, y, controlSize - 1, controlSize - 1, 2, 2);
            } else {
                g.setColor(backgroundInactive);
                g.fillRoundRect(x, y, controlSize - 1, controlSize - 1, 2, 2);
                g.setColor(ModernMetalTheme.DARK_GRAY);
                g.drawRoundRect(x, y, controlSize - 2, controlSize - 2, 2, 2);
                g.setColor(c.getForeground());
            }
        } else {
            g.setColor(MetalLookAndFeel.getControlShadow());
            g.drawRect(x, y, controlSize - 2, controlSize - 2);
        }

        if (model.isSelected()) {
            g.setColor(Color.WHITE);
            drawCheck(c, g, x, y);
        }

    }

    protected void drawCheck(Component c, Graphics g, int x, int y) {
        int controlSize = getControlSize();
//        g.fillRect( x+3, y+5, 2, controlSize-8 );
//        g.drawLine( x+(controlSize-4), y+3, x+5, y+(controlSize-6) );
//        g.drawLine( x+(controlSize-4), y+4, x+5, y+(controlSize-5) );
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));
        int xs = 1;
        int ys = 1;
        int x0 = 2;
        int y0 = 6;
        int x1 = 5;
        int y1 = 9;
        int x2 = 10;
        int y2 = 2;
        g.drawLine(x + x0 + xs, y + y0 + ys, x + x1 + xs, y + y1 + ys);
        g.drawLine(x + x2 + xs, y + y2 + ys, x + x1 + xs, y + y1 + ys);
//        g.drawLine(x + controlSize - d, y + d, x + controlSize / 2, y + controlSize - 6);
    }

    public int getIconWidth() {
        return getControlSize();
    }

    public int getIconHeight() {
        return getControlSize();
    }
}
