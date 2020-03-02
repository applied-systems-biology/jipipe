/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.components;

import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

/**
 * Icon that is only one specific color, including border
 */
public class ColorIcon implements Icon {
    private int imageWidth;
    private int imageHeight;

    private Color color;
    private Color border;
    private Insets insets;

    public ColorIcon() {
        this(16, 16);
    }

    public ColorIcon(int width, int height) {
        this(width, height, Color.black);
    }

    public ColorIcon(int width, int height, Color c) {
        imageWidth = width;
        imageHeight = height;

        color = c;
        border = Color.black;
        insets = new Insets(1, 1, 1, 1);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color c) {
        color = c;
    }

    public void setBorderColor(Color c) {
        border = c;
    }

    public int getIconWidth() {
        return imageWidth;
    }

    public int getIconHeight() {
        return imageHeight;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(border);
        g.drawRect(x, y, imageWidth - 1, imageHeight - 2);

        x += insets.left;
        y += insets.top;

        int w = imageWidth - insets.left - insets.right;
        int h = imageHeight - insets.top - insets.bottom - 1;

        g.setColor(color);
        g.fillRect(x, y, w, h);
    }
}