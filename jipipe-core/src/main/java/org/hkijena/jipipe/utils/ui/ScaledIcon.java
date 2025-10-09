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

import javax.swing.*;
import java.awt.*;

public class ScaledIcon implements Icon {
    private final Icon baseIcon;
    private final double scale;

    public ScaledIcon(Icon baseIcon, double scale) {
        if (baseIcon == null) {
            throw new IllegalArgumentException("Base icon cannot be null");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale factor must be positive");
        }
        this.baseIcon = baseIcon;
        this.scale = scale;
    }

    @Override
    public int getIconWidth() {
        return (int) Math.round(baseIcon.getIconWidth() * scale);
    }

    @Override
    public int getIconHeight() {
        return (int) Math.round(baseIcon.getIconHeight() * scale);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.translate(x, y);
            g2.scale(scale, scale);
            baseIcon.paintIcon(c, g2, 0, 0);
        } finally {
            g2.dispose();
        }
    }

    public Icon getBaseIcon() {
        return baseIcon;
    }

    public double getScale() {
        return scale;
    }
}

