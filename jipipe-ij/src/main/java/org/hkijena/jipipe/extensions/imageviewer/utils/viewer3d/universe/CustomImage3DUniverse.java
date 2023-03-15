package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.universe;

import com.google.common.collect.ImmutableList;
import ij3d.Image3DUniverse;
import ij3d.pointlist.PointListDialog;
import org.hkijena.jipipe.utils.ReflectionUtils;

import javax.swing.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class CustomImage3DUniverse extends Image3DUniverse {
    private final CustomInteractiveViewPlatformTransformer customInteractiveViewPlatformTransformer;

    public CustomImage3DUniverse() {
        customInteractiveViewPlatformTransformer = new CustomInteractiveViewPlatformTransformer(this, this);

        // Hack the point list dialog, because this is not what we do here
        ReflectionUtils.setDeclaredFieldValue(Image3DUniverse.class, this, "plDialog", new PointListDialog(null));

        // Remove context menu and other listeners from the canvas
        for (MouseListener listener : ImmutableList.copyOf(getCanvas().getMouseListeners())) {
            if(listener.getClass().getName().startsWith("ij3d.Image3DUniverse")) {
                getCanvas().removeMouseListener(listener);
            }
        }
        for (MouseMotionListener listener : ImmutableList.copyOf(getCanvas().getMouseMotionListeners())) {
            if(listener.getClass().getName().startsWith("ij3d.Image3DUniverse")) {
                getCanvas().removeMouseMotionListener(listener);
            }
        }

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
