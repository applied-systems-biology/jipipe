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

package org.hkijena.jipipe.utils.ui;

import javax.swing.border.AbstractBorder;
import java.awt.*;

/**
 * Rounded line border that actually works
 * <a href="https://stackoverflow.com/questions/52759203/rounded-lineborder-not-all-corners-are-rounded">...</a>
 */
public class RoundedLineBorder extends AbstractBorder {
    private int lineSize, cornerSize;
    private Paint fill;
    private Stroke stroke;
    private Object aaHint;

    public RoundedLineBorder(Paint fill, int lineSize, int cornerSize) {
        this.fill = fill;
        this.lineSize = lineSize;
        this.cornerSize = cornerSize;
        stroke = new BasicStroke(lineSize);
    }

    public RoundedLineBorder(Paint fill, int lineSize, int cornerSize, boolean antiAlias) {
        this.fill = fill;
        this.lineSize = lineSize;
        this.cornerSize = cornerSize;
        stroke = new BasicStroke(lineSize);
        aaHint = antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        int size = Math.max(lineSize, cornerSize);
        if (insets == null) insets = new Insets(size, size, size, size);
        else insets.left = insets.top = insets.right = insets.bottom = size;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        Paint oldPaint = g2d.getPaint();
        Stroke oldStroke = g2d.getStroke();
        Object oldAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        try {
            g2d.setPaint(fill != null ? fill : c.getForeground());
            g2d.setStroke(stroke);
            if (aaHint != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaHint);
            int off = lineSize >> 1;
            g2d.drawRoundRect(x + off, y + off, width - lineSize, height - lineSize, cornerSize, cornerSize);
        } finally {
            g2d.setPaint(oldPaint);
            g2d.setStroke(oldStroke);
            if (aaHint != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAA);
        }
    }

    public int getLineSize() {
        return lineSize;
    }

    public void setLineSize(int lineSize) {
        this.lineSize = lineSize;
        this.stroke = new BasicStroke(lineSize);
    }

    public int getCornerSize() {
        return cornerSize;
    }

    public void setCornerSize(int cornerSize) {
        this.cornerSize = cornerSize;
    }

    public Paint getFill() {
        return fill;
    }

    public void setFill(Paint fill) {
        this.fill = fill;
    }
}
