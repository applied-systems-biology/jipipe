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

package org.hkijena.jipipe.plugins.parameters.library.colors;

import javax.swing.*;
import java.awt.*;

/**
 * Icon that renders a color map entry
 */
public class ColorMapIcon implements Icon {
    private int imageWidth;
    private int imageHeight;
    private Color borderColor = Color.BLACK;
    private ColorMap colorMap;

    private Insets insets;

    /**
     * Creates a 16x16 black icon
     */
    public ColorMapIcon() {
        this(32, 16);
    }

    /**
     * Creates a viridis icon
     *
     * @param width  icon width
     * @param height icon height
     */
    public ColorMapIcon(int width, int height) {
        this(width, height, ColorMap.viridis);
    }

    /**
     * @param width    icon width
     * @param height   icon height
     * @param colorMap the color map
     */
    public ColorMapIcon(int width, int height, ColorMap colorMap) {
        imageWidth = width;
        imageHeight = height;
        this.colorMap = colorMap;
        insets = new Insets(1, 1, 1, 1);
    }

    public int getIconWidth() {
        return imageWidth;
    }

    public int getIconHeight() {
        return imageHeight;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(borderColor);
        g.drawRect(x, y, imageWidth - 1, imageHeight - 2);

        x += insets.left;
        y += insets.top;

        int w = imageWidth - insets.left - insets.right;
        int h = imageHeight - insets.top - insets.bottom - 1;

        g.drawImage(colorMap.getMapImage(), x, y, w, h, Color.WHITE, null);
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    public ColorMap getColorMap() {
        return colorMap;
    }

    public void setColorMap(ColorMap colorMap) {
        this.colorMap = colorMap;
    }
}