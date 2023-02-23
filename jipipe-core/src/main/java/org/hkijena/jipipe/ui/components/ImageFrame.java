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

import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageFrame extends JPanel {

    private boolean withGrid;
    private Mode mode;
    private boolean center;
    private BufferedImage backgroundImage;

    private double scaleFactor = 1;

    public ImageFrame(BufferedImage backgroundImage, boolean withGrid, Mode mode, boolean center) {
        this.backgroundImage = backgroundImage;
        this.withGrid = withGrid;
        this.mode = mode;
        this.center = center;
        setOpaque(false);
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
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

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public boolean isCenter() {
        return center;
    }

    public void setCenter(boolean center) {
        this.center = center;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        if(isOpaque()) {
            if (UIUtils.DARK_THEME) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (backgroundImage != null) {
            int newWidth, newHeight;
            double factorH = 1.0 * getHeight() / backgroundImage.getHeight();
            double factorW = 1.0 * getWidth() / backgroundImage.getWidth();
            switch (mode) {
                case Cover: {
                    double factor = Math.max(factorH, factorW) * scaleFactor;
                    newWidth = (int)(factor * backgroundImage.getWidth());
                    newHeight = (int)(factor * backgroundImage.getHeight());
                }
                break;
                case FitHeight: {
                    newWidth = (int)(factorH * scaleFactor * backgroundImage.getWidth());
                    newHeight = (int)(factorH * scaleFactor * backgroundImage.getHeight());
                }
                break;
                case FitWidth: {
                    newWidth = (int)(factorW * scaleFactor * backgroundImage.getWidth());
                    newHeight = (int)(factorW * scaleFactor * backgroundImage.getHeight());
                }
                break;
                case Fit: {
                    double factor = Math.min(factorH, factorW) * scaleFactor;
                    newWidth = (int)(factor * backgroundImage.getWidth());
                    newHeight = (int)(factor * backgroundImage.getHeight());
                }
                break;
                default:
                    throw new IllegalStateException();
            }
            if(center) {
                g.drawImage(backgroundImage, getWidth() / 2 - newWidth / 2, getHeight() / 2 - newHeight / 2, newWidth, newHeight, null);
            }
            else {
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

    public enum Mode {
        Cover,
        Fit,
        FitWidth,
        FitHeight
    }
}
