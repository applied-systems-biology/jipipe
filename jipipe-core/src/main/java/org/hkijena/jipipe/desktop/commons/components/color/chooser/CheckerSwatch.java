package org.hkijena.jipipe.desktop.commons.components.color.chooser;

import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * A color swatch with a checkerboard behind it (shows alpha).
 */
public class CheckerSwatch extends JPanel {
    private Color c;
    private final JLabel label = new JLabel();

    public CheckerSwatch() {
        setLayout(new BorderLayout());
        label.setBorder(UIUtils.createEmptyBorder(8));
        add(label, BorderLayout.CENTER);
    }

    public void setColor(Color c) {
        this.c = c;
        this.label.setText(String.format("RGB %d,%d,%d #%02x%02x%02x",  c.getRed(), c.getGreen(), c.getBlue(), c.getRed(), c.getGreen(), c.getBlue()));
        this.label.setForeground(ColorUtils.getContrastColor(c));
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        final int m = 0;
        final int a = ThemeUtils.getCurrentStyle().getIslandsCornerRadius();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D clip = new RoundRectangle2D.Double(m, m, getWidth() - 2*m, getHeight()-2*m, a, a);
        g2d.setClip(clip);
        int w = getWidth(), h = getHeight();
        int sq = 6;
        for (int y = 0; y < h; y += sq) {
            for (int x = 0; x < w; x += sq) {
                boolean light = ((x / sq + y / sq) & 1) == 0;
                g.setColor(light ? new Color(0xdd, 0xdd, 0xdd) : new Color(0xbb, 0xbb, 0xbb));
                g.fillRect(x, y, Math.min(sq, w - x), Math.min(sq, h - y));
            }
        }
        if (c != null) {
            g.setColor(c);
            g.fillRect(0, 0, w, h);
        }
        g2d.setClip(null);
        g.setColor(ThemeUtils.getCurrentStyle().getBorderColor());
        g.drawRoundRect(m, m, w - 1 - 2 * m, h - 1 - 2 * m, a, a);
    }
}
