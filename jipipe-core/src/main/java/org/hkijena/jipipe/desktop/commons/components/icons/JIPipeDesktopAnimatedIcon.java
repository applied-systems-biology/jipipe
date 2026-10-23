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

package org.hkijena.jipipe.desktop.commons.components.icons;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeApplicationSettingsServiceComponent;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopAnimatedIcon implements Icon, JIPipeApplicationSettingsServiceComponent.ChangedEventListener {

    private final ImageIcon topIcon;
    private final Timer timer;
    private Component parent;
    private ImageIcon bottomIcon;
    private double opacity;
    private double opacityStep;
    private boolean reducedMotion;
    private boolean shouldAnimate = false;

    public JIPipeDesktopAnimatedIcon(Component parent, ImageIcon bottomIcon, ImageIcon topIcon, int delay, double opacityStep) {
        this.parent = parent;
        this.bottomIcon = bottomIcon;
        this.topIcon = topIcon;
        this.timer = new Timer(delay, e -> updateIcon());
        this.opacityStep = opacityStep;
        this.timer.setRepeats(true);
        this.timer.setCoalesce(false);
        setReducedMotion(ThemeUtils.isReducedMotion());
        try {
            JIPipe.getSettings().getChangedEventEmitter().subscribeWeak(this);
        } catch (Throwable ignored) {
            // JIPipe not initialized (headless/test) — no live re-apply needed.
        }
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
        if (parent != null && parent.isDisplayable() && parent.isVisible()) {
            parent.repaint();
            parent.getToolkit().sync();
        }
    }

    public void start() {
        shouldAnimate = true;
        if (!reducedMotion) {
            timer.start();
        }
        if (parent != null) {
            parent.repaint();
        }
    }

    public void stop() {
        shouldAnimate = false;
        timer.stop();
    }

    public boolean isReducedMotion() {
        return reducedMotion;
    }

    public double getOpacity() {
        return opacity;
    }

    public void setReducedMotion(boolean reducedMotion) {
        this.reducedMotion = reducedMotion;
        if (reducedMotion) {
            if (timer.isRunning()) {
                timer.stop();
            }
            opacity = 1.0; // static: show the top (orange) icon fully
        } else {
            opacity = 0.0; // restart the cross-fade from the bottom icon
            if (shouldAnimate && !timer.isRunning()) {
                // Setting turned off while a notification is still present: resume.
                timer.start();
            }
        }
        if (parent != null) {
            parent.repaint();
        }
    }

    @Override
    public void onApplicationSettingsChanged() {
        setReducedMotion(ThemeUtils.isReducedMotion());
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
