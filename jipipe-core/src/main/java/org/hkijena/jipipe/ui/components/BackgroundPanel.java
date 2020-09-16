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

package org.hkijena.jipipe.ui.components;

import javax.swing.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class BackgroundPanel extends JPanel {

    private final boolean withGrid;
    private BufferedImage backgroundImage;

    public BackgroundPanel(BufferedImage backgroundImage, boolean withGrid) {
        this.backgroundImage = backgroundImage;
        this.withGrid = withGrid;
        setOpaque(false);
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (backgroundImage != null) {
            double factorh = 1.0 * getHeight() / backgroundImage.getHeight();
            double factorw = 1.0 * getWidth() / backgroundImage.getWidth();
            double factor = Math.max(factorw, factorh);
            g.drawImage(backgroundImage, 0, 0, (int) (backgroundImage.getWidth() * factor), (int) (backgroundImage.getHeight() * factor), null);
        }

        if (withGrid) {
            final int gs = 25;
            g.setColor(new Color(185, 206, 227, 50));
            for (int x = gs; x < getWidth(); x += gs) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = gs; y < getHeight(); y += gs) {
                g.drawLine(0, y, getWidth(), y);
            }
        }

        super.paint(g);
    }
}
