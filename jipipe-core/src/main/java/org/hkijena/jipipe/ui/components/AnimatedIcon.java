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
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class AnimatedIcon implements Icon {

    private final ImageIcon topIcon;
    private final Timer timer;
    private Component parent;
    private ImageIcon bottomIcon;
    private double opacity;
    private double opacityStep;

    public AnimatedIcon(Component parent, ImageIcon bottomIcon, ImageIcon topIcon, int delay, double opacityStep) {
        this.parent = parent;
        this.bottomIcon = bottomIcon;
        this.topIcon = topIcon;
        this.timer = new Timer(delay, e -> updateIcon());
        this.opacityStep = opacityStep;
        this.timer.setRepeats(true);
        this.timer.setCoalesce(false);
    }

    public Component getParent() {
        return parent;
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    public ImageIcon getBottomIcon() {
        return bottomIcon;
    }

    public void setBottomIcon(ImageIcon bottomIcon) {
        this.bottomIcon = bottomIcon;
        updateIcon();
    }

    public double getOpacityStep() {
        return opacityStep;
    }

    public void setOpacityStep(double opacityStep) {
        this.opacityStep = opacityStep;
    }

    public Timer getTimer() {
        return timer;
    }

    private void updateIcon() {
        opacity = opacity + opacityStep;
        if (opacity >= 1) {
            opacity = 1;
            opacityStep = -opacityStep;
        } else if (opacity <= 0) {
            opacity = 0;
            opacityStep = -opacityStep;
        }
        if (parent != null) {
            parent.repaint();
            parent.getToolkit().sync();
        }
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(bottomIcon.getImage(), x, y, null);
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
        graphics2D.drawImage(topIcon.getImage(), x, y, null);
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    @Override
    public int getIconWidth() {
        return bottomIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return bottomIcon.getIconHeight();
    }
}
