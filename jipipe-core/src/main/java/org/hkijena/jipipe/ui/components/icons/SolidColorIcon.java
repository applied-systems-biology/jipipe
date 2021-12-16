/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.components.icons;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Icon that is only one specific color, including border
 */
public class SolidColorIcon implements ColorIcon {
    private final int imageWidth;
    private final int imageHeight;

    private Color fillColor;
    private Color borderColor;
    private final Insets insets;

    /**
     * Creates a 16x16 black icon
     */
    public SolidColorIcon() {
        this(16, 16);
    }

    /**
     * Creates a black icon
     *
     * @param width  icon width
     * @param height icon height
     */
    public SolidColorIcon(int width, int height) {
        this(width, height, Color.black);
    }

    /**
     * Creates an icon with black borders
     *
     * @param imageWidth  icon width
     * @param imageHeight icon height
     * @param fillColor   fill color
     */
    public SolidColorIcon(int imageWidth, int imageHeight, Color fillColor) {
        this(imageWidth, imageHeight, fillColor, Color.BLACK);
    }

    /**
     * @param width       icon width
     * @param height      icon height
     * @param fillColor   fill color
     * @param borderColor border color
     */
    public SolidColorIcon(int width, int height, Color fillColor, Color borderColor) {
        imageWidth = width;
        imageHeight = height;

        this.fillColor = fillColor;
        this.borderColor = borderColor;
        insets = new Insets(1, 1, 1, 1);
    }

    public Color getFillColor() {
        return fillColor;
    }

    public void setFillColor(Color c) {
        fillColor = c;
    }

    public void setBorderColor(Color c) {
        borderColor = c;
    }

    @Override
    public Color getBorderColor() {
        return borderColor;
    }

    @Override
    public int getIconWidth() {
        return imageWidth;
    }

    @Override
    public int getIconHeight() {
        return imageHeight;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(borderColor);
        g.drawRect(x, y, imageWidth - 1, imageHeight - 2);

        x += insets.left;
        y += insets.top;

        int w = imageWidth - insets.left - insets.right;
        int h = imageHeight - insets.top - insets.bottom - 1;

        g.setColor(fillColor);
        g.fillRect(x, y, w, h);
    }
}