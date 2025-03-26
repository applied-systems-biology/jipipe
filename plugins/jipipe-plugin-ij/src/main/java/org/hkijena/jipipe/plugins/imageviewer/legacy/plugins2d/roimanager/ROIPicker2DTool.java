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

package org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.roimanager;

import ij.gui.Roi;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2DTool;
import org.hkijena.jipipe.utils.ui.events.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ROIPicker2DTool implements ImageViewerPanelCanvas2DTool, MouseClickedEventListener, MousePressedEventListener, MouseDraggedEventListener, MouseExitedEventListener, MouseReleasedEventListener {

    public static final Stroke STROKE_MARQUEE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);

    private final ROIManagerPlugin2D roiManagerPlugin;

    private Point selectionFirst;

    private Point selectionSecond;

    public ROIPicker2DTool(ROIManagerPlugin2D roiManagerPlugin) {
        this.roiManagerPlugin = roiManagerPlugin;
        ImageViewerPanelCanvas2D canvas = roiManagerPlugin.getViewerPanel2D().getCanvas();
        canvas.getMouseClickedEventEmitter().subscribe(this);
        canvas.getMousePressedEventEmitter().subscribe(this);
        canvas.getMouseDraggedEventEmitter().subscribe(this);
        canvas.getMouseExitedEventEmitter().subscribe(this);
        canvas.getMouseReleasedEventEmitter().subscribe(this);
    }

    public ImageViewerPanelCanvas2D getCanvas() {
        return roiManagerPlugin.getViewerPanel2D().getCanvas();
    }

    @Override
    public Cursor getToolCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void onToolActivate(ImageViewerPanelCanvas2D canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas2D canvas) {
        cancelPicking();
    }

    private boolean toolIsActive() {
        return toolIsActive(roiManagerPlugin.getViewerPanel2D().getCanvas());
    }

    @Override
    public void onComponentMouseClicked(MouseClickedEvent event) {
        if (!toolIsActive()) {
            return;
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
            selectionFirst = event.getPoint();
            selectionSecond = event.getPoint();
            pickRoiFromCanvas(event.isShiftDown());
        }
        cancelPicking();
    }

    private void pickRoiFromCanvas(boolean modify) {
        if (selectionFirst != null && selectionSecond != null) {
            ImageViewerPanelCanvas2D canvas = roiManagerPlugin.getViewerPanel2D().getCanvas();
            Point p1 = canvas.screenToImageCoordinate(selectionFirst, false);
            Point p2 = canvas.screenToImageCoordinate(selectionSecond, false);
            int x0 = p1.x;
            int y0 = p1.y;
            int x1 = p2.x;
            int y1 = p2.y;
            int x = Math.min(x0, x1);
            int y = Math.min(y0, y1);
            int w = Math.abs(x0 - x1);
            int h = Math.abs(y0 - y1);
            Rectangle selection = new Rectangle(x, y, Math.max(1, w), Math.max(1, h));
            List<Roi> currentSelection = roiManagerPlugin.getRoiListControl().getSelectedValuesList();
            Set<Roi> toSelect = new HashSet<>();
            Set<Roi> toDeselect = new HashSet<>();
            for (Roi roi : roiManagerPlugin.getRoiDrawer().filterVisibleROI(roiManagerPlugin.getRois(), roiManagerPlugin.getCurrentSlicePosition())) {
                if (roi.getPolygon().intersects(selection)) {
                    if (modify) {
                        if (currentSelection.contains(roi)) {
                            toDeselect.add(roi);
                        } else {
                            toSelect.add(roi);
                        }
                    } else {
                        toSelect.add(roi);
                    }
                }
            }
            if (modify) {
                toSelect.addAll(currentSelection);
                toSelect.removeAll(toDeselect);
            }
            roiManagerPlugin.setSelectedROI(toSelect, true);
        }
    }


    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (selectionFirst != null && selectionSecond != null) {
            graphics2D.setStroke(STROKE_MARQUEE);
            graphics2D.setColor(Color.WHITE);
            int x0 = selectionFirst.x;
            int y0 = selectionFirst.y;
            int x1 = selectionSecond.x;
            int y1 = selectionSecond.y;
            int x = Math.min(x0, x1);
            int y = Math.min(y0, y1);
            int w = Math.abs(x0 - x1);
            int h = Math.abs(y0 - y1);
            graphics2D.drawRect(x, y, w, h);
            graphics2D.setColor(Color.BLACK);
            graphics2D.drawRect(x - 1, y - 1, w + 2, h + 2);
        }
    }

    @Override
    public void onComponentMousePressed(MousePressedEvent event) {
        if (toolIsActive() && SwingUtilities.isLeftMouseButton(event)) {
            selectionFirst = event.getPoint();
        }
    }

    @Override
    public void onComponentMouseReleased(MouseReleasedEvent event) {
        if (toolIsActive()) {
            pickRoiFromCanvas(event.isShiftDown());
        }
        cancelPicking();
    }

    @Override
    public void onComponentMouseExited(MouseExitedEvent event) {
        cancelPicking();
    }

    @Override
    public void onComponentMouseDragged(MouseDraggedEvent event) {
        if (toolIsActive() && selectionFirst != null) {
            selectionSecond = event.getPoint();
            roiManagerPlugin.getViewerPanel2D().uploadSliceToCanvas();
        }
    }

    private void cancelPicking() {
        selectionFirst = null;
        selectionSecond = null;
        roiManagerPlugin.getViewerPanel2D().uploadSliceToCanvas();
    }

    @Override
    public String getToolName() {
        return "Pick ROI";
    }
}
