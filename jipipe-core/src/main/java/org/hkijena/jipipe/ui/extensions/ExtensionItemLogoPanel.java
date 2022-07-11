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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.ResourceUtils;

import javax.swing.*;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.IdentityHashMap;
import java.util.Map;

public class ExtensionItemLogoPanel extends JPanel {

    private static final Map<BufferedImage, BufferedImage> THUMBNAIL_CACHE = new IdentityHashMap<>();

    private static final Map<BufferedImage, BufferedImage> THUMBNAIL_DISABLED_CACHE = new IdentityHashMap<>();

    private final JIPipeDependency extension;
    private BufferedImage thumbnail;

    private BufferedImage thumbnailDeactivated;

    public ExtensionItemLogoPanel(JIPipeDependency extension) {
        this.extension = extension;
        setOpaque(false);
        initializeThumbnail();
    }

    private void initializeThumbnail() {
        if(extension.getMetadata().getThumbnail() != null && extension.getMetadata().getThumbnail().getImage() != null) {
            thumbnail = extension.getMetadata().getThumbnail().getImage();
        }
        else {
            thumbnail = new ImageParameter(ResourceUtils.getPluginResource("extension-thumbnail-default.png")).getImage();
        }
        BufferedImage originalThumbnail = thumbnail;
        BufferedImage cachedThumbnail = THUMBNAIL_CACHE.getOrDefault(thumbnail, null);
        if(cachedThumbnail != null) {
            thumbnailDeactivated = THUMBNAIL_DISABLED_CACHE.get(originalThumbnail);
            thumbnail = cachedThumbnail;
        }
        else {
            thumbnail = BufferedImageUtils.scaleImageToFit(thumbnail, 350,350);
            thumbnail = BufferedImageUtils.spatialBlurLinear(thumbnail, new Point(0,0), new Point(thumbnail.getWidth(), thumbnail.getHeight()), 20);
            thumbnailDeactivated = BufferedImageUtils.toBufferedImage(thumbnail, BufferedImage.TYPE_BYTE_GRAY);
            THUMBNAIL_CACHE.put(originalThumbnail, thumbnail);
            THUMBNAIL_DISABLED_CACHE.put(originalThumbnail, thumbnailDeactivated);
        }

    }

    private boolean drawExtensionActivated() {
        if( JIPipe.getInstance().getExtensionRegistry().getActivatedExtensions().contains(extension.getDependencyId())) {
            return !JIPipe.getInstance().getExtensionRegistry().getScheduledDeactivateExtensions().contains(extension.getDependencyId());
        }
        else {
            return JIPipe.getInstance().getExtensionRegistry().getScheduledActivateExtensions().contains(extension.getDependencyId());
        }
    }

    @Override
    public void paint(Graphics g) {
        double scale = Math.max(1.0 * getWidth() / thumbnail.getWidth(), 1.0 * getHeight() / thumbnail.getHeight());
        int targetWidth = (int) (scale * thumbnail.getWidth());
        int targetHeight = (int) (scale * thumbnail.getHeight());
        int x = getWidth() / 2 - targetWidth / 2;
        int y = getHeight() / 2 - targetHeight / 2;
        g.drawImage(drawExtensionActivated() ? thumbnail : thumbnailDeactivated, x,y,targetWidth,targetHeight,null);
        super.paint(g);
    }
}
