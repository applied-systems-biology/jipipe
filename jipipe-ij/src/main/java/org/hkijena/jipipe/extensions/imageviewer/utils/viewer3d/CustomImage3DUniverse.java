package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import ij3d.Content;
import ij3d.Image3DUniverse;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.AxisAngle4d;

import javax.swing.*;

public class CustomImage3DUniverse extends Image3DUniverse {
    private final CustomInteractiveViewPlatformTransformer customInteractiveViewPlatformTransformer;

    public CustomImage3DUniverse() {
        customInteractiveViewPlatformTransformer = new CustomInteractiveViewPlatformTransformer(this, this);
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
