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
import java.awt.*;
import java.awt.geom.AffineTransform;

public class ThrobberIcon implements Icon {

    private final Timer timer;
    private Component parent;
    private ImageIcon wrappedIcon;
    private double rotation = 0;
    private double rotationStep;

    public ThrobberIcon(Component parent, ImageIcon wrappedIcon, int delay, double rotationStep) {
        this.parent = parent;
        this.wrappedIcon = wrappedIcon;
        this.timer = new Timer(delay, e -> updateIcon());
        this.timer.setRepeats(true);
        this.timer.setCoalesce(false);
        this.rotationStep = rotationStep;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public Component getParent() {
        return parent;
    }

    public void setParent(Component parent) {
        this.parent = parent;
    }

    public ImageIcon getWrappedIcon() {
        return wrappedIcon;
    }

    public void setWrappedIcon(ImageIcon wrappedIcon) {
        this.wrappedIcon = wrappedIcon;
        updateIcon();
    }

    public Timer getTimer() {
        return timer;
    }

    public double getRotationStep() {
        return rotationStep;
    }

    public void setRotationStep(double rotationStep) {
        this.rotationStep = rotationStep;
        updateIcon();
    }

    private void updateIcon() {
        rotation += rotationStep;
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
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.rotate(Math.PI * 2 * (rotation / 360), getIconWidth() / 2.0, getIconHeight() / 2.0);
        affineTransform.translate(x, y);
        Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(wrappedIcon.getImage(), affineTransform, null);
    }

    @Override
    public int getIconWidth() {
        return wrappedIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        return wrappedIcon.getIconHeight();
    }
}
