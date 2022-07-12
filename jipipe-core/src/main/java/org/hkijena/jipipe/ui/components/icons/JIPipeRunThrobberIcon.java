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

package org.hkijena.jipipe.ui.components.icons;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.ui.running.RunWorkerFinishedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerInterruptedEvent;
import org.hkijena.jipipe.ui.running.RunWorkerStartedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

public class JIPipeRunThrobberIcon implements Icon {

    public static final int ANIMATION_DELAY = 80;
    public static final int ANIMATION_STEP = 24;

    private final Timer timer;
    private Component parent;
    private ImageIcon wrappedIcon;
    private double rotation = 0;
    private double rotationStep;

    public JIPipeRunThrobberIcon(Component parent) {
        this.parent = parent;
        this.wrappedIcon =  UIUtils.getIconFromResources("status/throbber.png");
        this.timer = new Timer(ANIMATION_DELAY, e -> updateIcon());
        this.timer.setRepeats(true);
        this.timer.setCoalesce(false);
        this.timer.stop();
        this.rotationStep = ANIMATION_STEP;

        JIPipeRunnerQueue.getInstance().getEventBus().register(this);
        if(!JIPipeRunnerQueue.getInstance().isEmpty()) {
            timer.start();
        }
    }

    @Subscribe
    public void onWorkerFinished(RunWorkerFinishedEvent event) {
        if(JIPipeRunnerQueue.getInstance().isEmpty()) {
            stop();
        }
    }

    @Subscribe
    public void onWorkerStart(RunWorkerStartedEvent event) {
        timer.start();
    }

    @Subscribe
    public void onWorkerInterrupted(RunWorkerInterruptedEvent event) {
        if(JIPipeRunnerQueue.getInstance().isEmpty()) {
            stop();
        }
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
        while(rotation > 360) {
            rotation -= 360;
        }
        if (parent != null && parent.isDisplayable()) {
            parent.repaint();
            parent.getToolkit().sync();
        }
        else {
            timer.stop();
        }
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
