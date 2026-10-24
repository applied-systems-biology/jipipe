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

package org.hkijena.jipipe.utils;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Small icon-composition helpers.
 */
public class IconUtils {

    /**
     * A green status dot ("complete / FAIR-ready").
     */
    public static final Color DOT_OK = new Color(0x2E, 0x7D, 0x32);
    /**
     * An amber status dot ("needs work").
     */
    public static final Color DOT_WARNING = new Color(0xF9, 0xA8, 0x25);

    /**
     * Draws a small filled status {@code color} dot into the bottom-right
     * corner of {@code base}, with a thin white ring so it stays readable
     * against any button background. Returns an {@link Icon} of the same size
     * as {@code base}. The dot is drawn (not an image overlay), so it is crisp
     * and theme-independent.
     *
     * @param base          the base icon (e.g. the publish share icon)
     * @param color         the status dot color (green = ok, amber = warning)
     * @param dotSizePixels the rendered diameter of the dot
     * @return the composited icon
     */
    public static Icon addBottomRightDot(Icon base, Color color, int dotSizePixels) {
        final int width = base.getIconWidth();
        final int height = base.getIconHeight();
        final int dot = Math.max(4, Math.min(dotSizePixels, Math.min(width, height) / 2));
        final int ring = Math.max(1, dot / 4);

        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                base.paintIcon(c, g, x, y);
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int dx = x + width - dot;
                    int dy = y + height - dot;
                    // White ring (halo) behind the dot.
                    g2.setColor(Color.WHITE);
                    g2.fillOval(dx - ring, dy - ring, dot + 2 * ring, dot + 2 * ring);
                    g2.setColor(color);
                    g2.fillOval(dx, dy, dot, dot);
                } finally {
                    g2.dispose();
                }
            }

            @Override
            public int getIconWidth() {
                return width;
            }

            @Override
            public int getIconHeight() {
                return height;
            }
        };
    }
}
