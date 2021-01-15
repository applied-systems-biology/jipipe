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

package org.hkijena.jipipe.extensions.plots.utils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

public class ColorMapIcon implements Icon {

    private final int width;
    private final int height;
    private final ColorMap colorMap;

    public ColorMapIcon(int width, int height, ColorMap colorMap) {
        this.width = width;
        this.height = height;
        this.colorMap = colorMap;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        int rw = width;
        int step = (int) Math.ceil(1.0 * width / colorMap.getColors().length);
        int cx = x;
        int i = 0;
        while (rw > 0) {
            int w = Math.min(step, rw);
            g2.setPaint(colorMap.getColors()[i]);
            g2.fillRect(cx, y, w, height);
            rw -= w;
            cx += w;
            ++i;
        }
    }

    public ColorMap getColorMap() {
        return colorMap;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }
}
