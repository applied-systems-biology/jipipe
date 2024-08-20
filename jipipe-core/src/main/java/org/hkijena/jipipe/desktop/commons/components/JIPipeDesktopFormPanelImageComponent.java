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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.utils.SizeFitMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class JIPipeDesktopFormPanelImageComponent extends JPanel {

    private boolean center;
    private BufferedImage backgroundImage;
    private double scaleFactor = 1.0;

    public JIPipeDesktopFormPanelImageComponent(BufferedImage backgroundImage, boolean center) {
        this.backgroundImage = backgroundImage;
        this.center = center;
        setOpaque(false);
//        setBorder(BorderFactory.createLineBorder(Color.RED));
    }

    public BufferedImage getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(BufferedImage backgroundImage) {
        this.backgroundImage = backgroundImage;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();
        if (backgroundImage != null) {
            double width = Math.max(preferredSize.getWidth(), getWidth());
            double factor = width / backgroundImage.getWidth();
//            System.out.println(preferredSize + " " + getWidth() + " " + factor + " " + backgroundImage.getHeight()  * factor);
            return new Dimension((int) (width * factor), (int) (backgroundImage.getHeight() * factor));
        }
        return preferredSize;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
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
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isOpaque()) {
            if (UIUtils.DARK_THEME) {
                g.setColor(Color.BLACK);
            } else {
                g.setColor(Color.WHITE);
            }
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        if (backgroundImage != null) {
            Dimension newSize = SizeFitMode.FitWidth.fitSize(getWidth(), getHeight(), backgroundImage.getWidth(), backgroundImage.getHeight(), scaleFactor);
            int newWidth = newSize.width;
            int newHeight = newSize.height;
            if (center) {
                g.drawImage(backgroundImage, getWidth() / 2 - newWidth / 2, getHeight() / 2 - newHeight / 2, newWidth, newHeight, null);
            } else {
                g.drawImage(backgroundImage, 0, 0, newWidth, newHeight, null);
            }
        }

        super.paint(g);
    }

}
