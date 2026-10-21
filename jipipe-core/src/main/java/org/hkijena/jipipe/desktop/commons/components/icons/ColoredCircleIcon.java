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

package org.hkijena.jipipe.desktop.commons.components.icons;

import javax.swing.*;
import java.awt.*;

/**
 * A 24×24 circular icon that wraps a smaller (16×16) icon.
 * <p>
 * Draws a filled circle in {@code background} with a 1px circular border in
 * {@code borderColor}, then the wrapped icon centered on top. Used by the node
 * browser's function tree: function rows wrap the 16×16 function icon in the
 * function's node fill/border color (so the tree doubles as a legend for the
 * canvas node colors), while template rows use neutral theme colors.
 */
public class ColoredCircleIcon implements Icon {
    private static final int SIZE = 24;

    private final Icon inner;
    private Color background;
    private Color borderColor;

    /**
     * @param inner      the icon to center (expected 16×16)
     * @param background the circular fill
     * @param borderColor the circular border
     */
    public ColoredCircleIcon(Icon inner, Color background, Color borderColor) {
        this.inner = inner;
        this.background = background;
        this.borderColor = borderColor;
    }

    public void setFillColor(Color c) {
        this.background = c;
    }

    public Color getFillColor() {
        return background;
    }

    public void setBorderColor(Color c) {
        this.borderColor = c;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Circular background (node fill color) + circular border (node border color).
            // Round-to-nearest-px keeps the 1px ring crisp.
            int d = SIZE - 2;
            g2d.setColor(background);
            g2d.fillOval(x + 1, y + 1, d, d);
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawOval(x + 1, y + 1, d, d);

            // The wrapped icon, centered (a 16×16 icon lands at the 4,4 offset).
            if (inner != null) {
                int ix = x + (SIZE - inner.getIconWidth()) / 2;
                int iy = y + (SIZE - inner.getIconHeight()) / 2;
                inner.paintIcon(c, g2d, ix, iy);
            }
        } finally {
            g2d.dispose();
        }
    }
}
