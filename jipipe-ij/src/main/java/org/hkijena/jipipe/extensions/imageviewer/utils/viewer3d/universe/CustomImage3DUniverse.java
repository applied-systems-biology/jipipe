package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.universe;

import ij3d.Image3DUniverse;
import ij3d.pointlist.PointListDialog;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;

public class CustomImage3DUniverse extends Image3DUniverse {
    private final CustomInteractiveViewPlatformTransformer customInteractiveViewPlatformTransformer;

    public CustomImage3DUniverse() {
        customInteractiveViewPlatformTransformer = new CustomInteractiveViewPlatformTransformer(this, this);

        // Hack the point list dialog, because this is not what we do here
        ReflectionUtils.setDeclaredFieldValue(Image3DUniverse.class, this, "plDialog", new PointListDialog(null));
    }

    public CustomInteractiveViewPlatformTransformer getCustomInteractiveViewPlatformTransformer() {
        return customInteractiveViewPlatformTransformer;
    }

    /**
     * Fixes a weird rendering bug where the objects are displayed as stripes
     * Zoom in+out
     */
    public void fixWeirdRendering() {
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            customInteractiveViewPlatformTransformer.zoom(0.1);
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                customInteractiveViewPlatformTransformer.zoom(-0.1);
            });
        });
    }

}
