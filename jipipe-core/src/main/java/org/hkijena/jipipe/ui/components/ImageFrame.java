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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.utils.SizeFitMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageFrame extends JPanel {

    private boolean withGrid;
    private SizeFitMode mode;
    private boolean center;
    private BufferedImage backgroundImage;

    private double scaleFactor = 1;

    public ImageFrame(BufferedImage backgroundImage, boolean withGrid, SizeFitMode mode, boolean center) {
        this.backgroundImage = backgroundImage;
        this.withGrid = withGrid;
        this.mode = mode;
        this.center = center;
        setOpaque(false);
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
        repaint();
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public boolean isWithGrid() {
        return withGrid;
    }

    public void setWithGrid(boolean withGrid) {
        this.withGrid = withGrid;
    }

    public SizeFitMode getMode() {
        return mode;
    }

    public void setMode(SizeFitMode mode) {
        this.mode = mode;
    }

    public boolean isCenter() {
        return center;
    }

    public void setCenter(boolean center) {
        this.center = center;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if (isOpaque()) {
            if (UIUtils.DARK_THEME) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (backgroundImage != null) {
            Dimension newSize = mode.fitSize(getWidth(), getHeight(), backgroundImage.getWidth(), backgroundImage.getHeight(), scaleFactor);
            int newWidth = newSize.width;
            int newHeight = newSize.height;
            if (center) {
                g.drawImage(backgroundImage, getWidth() / 2 - newWidth / 2, getHeight() / 2 - newHeight / 2, newWidth, newHeight, null);
            } else {
                g.drawImage(backgroundImage, 0, 0, newWidth, newHeight, null);
            }
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
