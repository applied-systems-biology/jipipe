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

package org.hkijena.jipipe.desktop.app.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.registries.JIPipePluginRegistry;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopActivateAndApplyUpdateSiteRun;
import org.hkijena.jipipe.desktop.app.plugins.JIPipeDesktopDeactivateAndApplyUpdateSiteRun;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

public class JIPipeDesktopExtensionItemLogoPanel extends JPanel implements JIPipePluginRegistry.ScheduledActivatePluginEventListener, JIPipePluginRegistry.ScheduledDeactivatePluginEventListener, JIPipeRunnable.FinishedEventListener {

    private static final Map<BufferedImage, BufferedImage> THUMBNAIL_CACHE = new IdentityHashMap<>();

    private static final Map<BufferedImage, BufferedImage> THUMBNAIL_DISABLED_CACHE = new IdentityHashMap<>();

    private final JIPipePlugin extension;
    private BufferedImage thumbnail;

    private BufferedImage thumbnailDeactivated;

    public JIPipeDesktopExtensionItemLogoPanel(JIPipePlugin extension) {
        this.extension = extension;
        setOpaque(false);
        JIPipe.getInstance().getPluginRegistry().getScheduledActivatePluginEventEmitter().subscribeWeak(this);
        JIPipe.getInstance().getPluginRegistry().getScheduledDeactivatePluginEventEmitter().subscribeWeak(this);
        JIPipeRunnableQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
    }


    @Override
    public void paint(Graphics g) {
        double scale = Math.max(1.0 * getWidth() / thumbnail.getWidth(), 1.0 * getHeight() / thumbnail.getHeight());
        int targetWidth = (int) (scale * thumbnail.getWidth());
        int targetHeight = (int) (scale * thumbnail.getHeight());
        int x = getWidth() / 2 - targetWidth / 2;
        int y = getHeight() / 2 - targetHeight / 2;
        boolean displayMode;
        if (extension.isScheduledForActivation()) {
            displayMode = true;
        } else if (extension.isScheduledForDeactivation()) {
            displayMode = false;
        } else {
            displayMode = extension.isActivated();
        }
        g.drawImage(displayMode ? thumbnail : thumbnailDeactivated, x, y, targetWidth, targetHeight, null);
        super.paint(g);
    }

    @Override
    public void onScheduledActivatePlugin(JIPipePluginRegistry.ScheduledActivatePluginEvent event) {
        repaint(50);
    }

    @Override
    public void onScheduledDeactivatePlugin(JIPipePluginRegistry.ScheduledDeactivatePluginEvent event) {
        repaint(50);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof JIPipeDesktopActivateAndApplyUpdateSiteRun || event.getRun() instanceof JIPipeDesktopDeactivateAndApplyUpdateSiteRun) {
            repaint(50);
        }
    }
}
