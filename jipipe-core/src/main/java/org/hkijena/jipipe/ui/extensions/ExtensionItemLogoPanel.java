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

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.registries.JIPipeExtensionRegistry;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

public class ExtensionItemLogoPanel extends JPanel implements JIPipeExtensionRegistry.ScheduledActivateExtensionEventListener, JIPipeExtensionRegistry.ScheduledDeactivateExtensionEventListener, JIPipeRunnable.FinishedEventListener {

    private static final Map<BufferedImage, BufferedImage> THUMBNAIL_CACHE = new IdentityHashMap<>();

    private static final Map<BufferedImage, BufferedImage> THUMBNAIL_DISABLED_CACHE = new IdentityHashMap<>();

    private final JIPipePlugin extension;
    private BufferedImage thumbnail;

    private BufferedImage thumbnailDeactivated;

    public ExtensionItemLogoPanel(JIPipePlugin extension) {
        this.extension = extension;
        setOpaque(false);
        initializeThumbnail();
        JIPipe.getInstance().getExtensionRegistry().getScheduledActivateExtensionEventEmitter().subscribeWeak(this);
        JIPipe.getInstance().getExtensionRegistry().getScheduledDeactivateExtensionEventEmitter().subscribeWeak(this);
        JIPipeRunnerQueue.getInstance().getFinishedEventEmitter().subscribeWeak(this);
    }

    private void initializeThumbnail() {
        if (extension.getMetadata().getThumbnail() != null && extension.getMetadata().getThumbnail().getImage() != null) {
            thumbnail = extension.getMetadata().getThumbnail().getImage();
        } else {
            thumbnail = new ImageParameter(ResourceUtils.getPluginResource("extension-thumbnail-default.png")).getImage();
        }
        BufferedImage originalThumbnail = thumbnail;
        BufferedImage cachedThumbnail = THUMBNAIL_CACHE.getOrDefault(thumbnail, null);
        if (cachedThumbnail != null) {
            thumbnailDeactivated = THUMBNAIL_DISABLED_CACHE.get(originalThumbnail);
            thumbnail = cachedThumbnail;
        } else {
            thumbnail = BufferedImageUtils.scaleImageToFit(thumbnail, 350, 350);
            thumbnail = BufferedImageUtils.spatialBlurLinear(thumbnail, new Point(0, 0), new Point(thumbnail.getWidth(), thumbnail.getHeight()), 20);
            thumbnailDeactivated = BufferedImageUtils.toBufferedImage(thumbnail, BufferedImage.TYPE_BYTE_GRAY);
            THUMBNAIL_CACHE.put(originalThumbnail, thumbnail);
            THUMBNAIL_DISABLED_CACHE.put(originalThumbnail, thumbnailDeactivated);
        }

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
    public void onScheduledActivateExtension(JIPipeExtensionRegistry.ScheduledActivateExtensionEvent event) {
        repaint(50);
    }

    @Override
    public void onScheduledDeactivateExtension(JIPipeExtensionRegistry.ScheduledDeactivateExtensionEvent event) {
        repaint(50);
    }

    @Override
    public void onRunnableFinished(JIPipeRunnable.FinishedEvent event) {
        if (event.getRun() instanceof ActivateAndApplyUpdateSiteRun || event.getRun() instanceof DeactivateAndApplyUpdateSiteRun) {
            repaint(50);
        }
    }
}
