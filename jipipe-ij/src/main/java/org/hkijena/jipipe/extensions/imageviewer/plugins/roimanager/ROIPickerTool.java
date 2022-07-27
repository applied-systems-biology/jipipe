package org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager;

import com.google.common.eventbus.Subscribe;
import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelCanvas;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelCanvasTool;
import org.hkijena.jipipe.utils.ui.*;

import javax.swing.*;
import java.awt.*;

public class ROIPickerTool implements ImageViewerPanelCanvasTool {

    public static final Stroke STROKE_MARQUEE = new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{2}, 0);

    private final ROIManagerPlugin roiManagerPlugin;

    private Point selectionFirst;

    private Point selectionSecond;

    public ROIPickerTool(ROIManagerPlugin roiManagerPlugin) {
        this.roiManagerPlugin = roiManagerPlugin;
        roiManagerPlugin.getViewerPanel().getCanvas().getEventBus().register(this);
    }

    public ImageViewerPanelCanvas getCanvas() {
        return roiManagerPlugin.getViewerPanel().getCanvas();
    }

    @Override
    public Cursor getToolCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public void onToolActivate(ImageViewerPanelCanvas canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas canvas) {
        cancelPicking();
    }

    private boolean toolIsActive() {
        return toolIsActive(roiManagerPlugin.getViewerPanel().getCanvas());
    }

    @Subscribe
    public void onMouseClick(MouseClickedEvent event) {
        if (!toolIsActive()) {
            return;
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
            selectionFirst = event.getPoint();
            selectionSecond = event.getPoint();
            pickRoiFromCanvas();
        }
        cancelPicking();
    }

    private void pickRoiFromCanvas() {
        if(selectionFirst != null && selectionSecond != null) {
            ImageViewerPanelCanvas canvas = roiManagerPlugin.getViewerPanel().getCanvas();
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
            Rectangle selection = new Rectangle(x,y, Math.max(1, w), Math.max(1, h));
            ROIListData toSelect = new ROIListData();
            for (Roi roi : roiManagerPlugin.getRoiDrawer().filterVisibleROI(roiManagerPlugin.getRois(), roiManagerPlugin.getCurrentSlicePosition())) {
                if( roi.getPolygon().intersects(selection)) {
                    toSelect.add(roi);
                }
            }
            roiManagerPlugin.setSelectedROI(toSelect, true);
        }
    }



    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if(selectionFirst != null && selectionSecond != null) {
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

    @Subscribe
    public void onMouseDown(MousePressedEvent event) {
        if(toolIsActive() && SwingUtilities.isLeftMouseButton(event)) {
            selectionFirst = event.getPoint();
        }
    }

    @Subscribe
    public void onMouseUp(MouseReleasedEvent event) {
        if(toolIsActive()) {
            pickRoiFromCanvas();
        }
        cancelPicking();
    }

    @Subscribe
    public void onMouseExited(MouseExitedEvent event) {
        cancelPicking();
    }

    @Subscribe
    public void onMouseDrag(MouseDraggedEvent event) {
        if(toolIsActive() && selectionFirst != null) {
            selectionSecond = event.getPoint();
            roiManagerPlugin.getViewerPanel().uploadSliceToCanvas();
        }
    }

    private void cancelPicking() {
        selectionFirst = null;
        selectionSecond = null;
        roiManagerPlugin.getViewerPanel().uploadSliceToCanvas();
    }

    @Override
    public String getToolName() {
        return "Pick ROI";
    }
}