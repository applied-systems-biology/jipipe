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

import javax.swing.*;
import java.awt.*;

/**
 * Icon that overlays a color rectangle on top of another icon
 */
public class OverlayColorIcon implements ColorIcon {
    private final Icon baseIcon;
    private Rectangle insets;
    private Color fillColor = Color.BLACK;
    private Color borderColor = Color.BLACK;
    private boolean drawFill;
    private boolean drawBorder;

    public OverlayColorIcon(Icon baseIcon, Rectangle insets, boolean drawFill, boolean drawBorder) {
        this.baseIcon = baseIcon;
        this.insets = insets;
        this.drawFill = drawFill;
        this.drawBorder = drawBorder;
    }

    public boolean isDrawFill() {
        return drawFill;
    }

    public void setDrawFill(boolean drawFill) {
        this.drawFill = drawFill;
    }

    public boolean isDrawBorder() {
        return drawBorder;
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color c) {
        fillColor = c;
    }

    @Override
    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color c) {
        borderColor = c;
    }

    @Override
    public int getIconWidth() {
        return baseIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return baseIcon.getIconHeight();
    }

    public Icon getBaseIcon() {
        return baseIcon;
    }

    public Rectangle getInsets() {
        return insets;
    }

    public void setInsets(Rectangle insets) {
        this.insets = insets;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        baseIcon.paintIcon(c, g, x, y);
        if (drawFill) {
            g.setColor(fillColor);
            g.fillRect(x + insets.x, y + insets.y, insets.width, insets.height);
        }
        if (drawBorder) {
            g.setColor(borderColor);
            g.drawRect(x + insets.x, y + insets.y, insets.width, insets.height);
        }
    }
}