package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.universe;

import ij3d.Content;
import ij3d.behaviors.InteractiveBehavior;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel3D;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public class CustomInteractiveBehavior extends InteractiveBehavior {

    private final ImageViewerPanel3D imageViewerPanel3D;

    private long kineticZoomLastTick;

    public CustomInteractiveBehavior(ImageViewerPanel3D imageViewerPanel3D, CustomImage3DUniverse universe) {
        super(universe);
        this.imageViewerPanel3D = imageViewerPanel3D;
    }

    @Override
    protected void doProcess(MouseEvent e) {
        final int id = e.getID();
        final int mask = e.getModifiersEx();
        final Content c = univ.getSelected();
        CustomInteractiveViewPlatformTransformer viewTransformer = imageViewerPanel3D.getUniverse().getCustomInteractiveViewPlatformTransformer();

        if(id == MouseEvent.MOUSE_WHEEL) {
            MouseWheelEvent mouseWheelEvent = (MouseWheelEvent) e;
            double amount = -mouseWheelEvent.getUnitsToScroll();
            long currentTick = System.currentTimeMillis();
            long timeBetweenLastTicks = currentTick - kineticZoomLastTick;

            double x = Math.max(0, Math.min(500, timeBetweenLastTicks));
            double fac = Math.exp(-x/225.5) * 1.5;

            viewTransformer.zoom(amount * fac);
            kineticZoomLastTick = currentTick;
        }
        else if(id == MouseEvent.MOUSE_CLICKED) {

        }
        else if(id == MouseEvent.MOUSE_PRESSED) {
            viewTransformer.init(e);
        }
        else if(id == MouseEvent.MOUSE_DRAGGED) {
            if(SwingUtilities.isMiddleMouseButton(e)) {
                if(e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.translate(e);
                }
                else if(!e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.rotate(e);
                }
            }
            else  if(SwingUtilities.isLeftMouseButton(e)) {
                if(e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.translate(e);
                }
                else if(!e.isShiftDown() && !e.isControlDown()) {
                    viewTransformer.rotate(e);
                }
            }
            else if(SwingUtilities.isRightMouseButton(e)) {
                viewTransformer.translate(e);
            }
        }

        e.consume();
    }
}
