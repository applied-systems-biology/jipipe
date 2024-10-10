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

package org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.universe;

import ij3d.Content;
import ij3d.behaviors.InteractiveBehavior;
import org.hkijena.jipipe.plugins.imageviewer.legacy.impl.JIPipeDesktopLegacyImageViewerPanel3D;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class CustomInteractiveBehavior extends InteractiveBehavior {

    private final JIPipeDesktopLegacyImageViewerPanel3D imageViewerPanel3D;

    private long kineticZoomLastTick;

    public CustomInteractiveBehavior(JIPipeDesktopLegacyImageViewerPanel3D imageViewerPanel3D, CustomImage3DUniverse universe) {
        super(universe);
        this.imageViewerPanel3D = imageViewerPanel3D;
    }

    @Override
    protected void doProcess(MouseEvent e) {
        final int id = e.getID();
        final int mask = e.getModifiersEx();
        final Content c = univ.getSelected();
        CustomInteractiveViewPlatformTransformer viewTransformer = imageViewerPanel3D.getUniverse().getCustomInteractiveViewPlatformTransformer();

        if (id == MouseEvent.MOUSE_WHEEL) {
            MouseWheelEvent mouseWheelEvent = (MouseWheelEvent) e;
            double amount = -mouseWheelEvent.getUnitsToScroll();
            long currentTick = System.currentTimeMillis();
            long timeBetweenLastTicks = currentTick - kineticZoomLastTick;

            double x = Math.max(0, Math.min(500, timeBetweenLastTicks));
            double fac = Math.exp(-x / 225.5) * 1.5;

            viewTransformer.zoom(amount * fac);
            kineticZoomLastTick = currentTick;
        } else if (id == MouseEvent.MOUSE_CLICKED) {

        } else if (id == MouseEvent.MOUSE_PRESSED) {
            viewTransformer.init(e);
        } else if (id == MouseEvent.MOUSE_DRAGGED) {
            if (SwingUtilities.isMiddleMouseButton(e)) {
                if (e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.translate(e);
                } else if (!e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.rotate(e);
                }
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.translate(e);
                } else if (!e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.rotate(e);
                }
            } else if (SwingUtilities.isRightMouseButton(e)) {
                viewTransformer.translate(e);
            }
        }

        e.consume();
    }
}
