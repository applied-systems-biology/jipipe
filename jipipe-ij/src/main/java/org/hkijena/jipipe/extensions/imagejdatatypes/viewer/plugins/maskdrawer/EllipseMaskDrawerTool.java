package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.MouseClickedEvent;
import org.hkijena.jipipe.utils.MouseExitedEvent;
import org.hkijena.jipipe.utils.MouseMovedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Ellipse drawing
 * Allows left-click canvas dragging
 */
public class EllipseMaskDrawerTool extends MaskDrawerTool {

    private Point referencePoint;
    private JCheckBox startFromCenterToggle;
    private JCheckBox squareToggle;
    private JCheckBox fillToggle;

    public EllipseMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin,
                "Ellipse",
                "Draws an ellipse between two points",
                UIUtils.getIconFromResources("actions/draw-ellipse-whole.png"));
        getViewerPanel().getCanvas().getEventBus().register(this);
        initialize();
    }

    private void initialize() {
        startFromCenterToggle = new JCheckBox("Start from center", false);
        squareToggle = new JCheckBox("Draw square", false);
        fillToggle = new JCheckBox("Fill", true);
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        formPanel.addToForm(startFromCenterToggle, new JLabel(), null);
        formPanel.addToForm(squareToggle, new JLabel(), null);
        formPanel.addToForm(fillToggle, new JLabel(), null);
    }

    @Override
    public void activate() {
        getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        getViewerPanel().getCanvas().setDragWithLeftMouse(false);
    }

    @Override
    public void deactivate() {
        cancelDrawing();
    }

    private void cancelDrawing() {
        referencePoint = null;
        getViewerPanel().getCanvas().repaint();
    }

    @Subscribe
    public void onMouseClick(MouseClickedEvent event) {
        if(!isActive())
            return;
        if(SwingUtilities.isLeftMouseButton(event)) {
            Point point = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
            if(point == null) {
                cancelDrawing();
                return;
            }

            if(referencePoint == null) {
                referencePoint = point;
            }
            else {
                applyDrawing(referencePoint, point);
                cancelDrawing();
            }
        }
        else if(SwingUtilities.isRightMouseButton(event)) {
            cancelDrawing();
        }
    }

    private void applyDrawing(Point p0, Point p1) {
        ImageProcessor processor = getMaskDrawerPlugin().getCurrentMaskSlice();
        processor.setValue(getMaskDrawerPlugin().getCurrentColor().getValue());

        Rectangle r = RectangleMaskDrawerTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        if(fillToggle.isSelected()) {
            processor.fillOval(r.x, r.y, r.width, r.height);
        }
        else {
            processor.drawOval(r.x, r.y, r.width, r.height);
        }
        getMaskDrawerPlugin().recalculateMaskPreview();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        if(referencePoint == null)
            return;
        Point p0 = referencePoint;
        Point p1 = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
        if(p1 == null)
            return;
        final double zoom = getViewerPanel().getCanvas().getZoom();
        graphics2D.setColor(getMaskDrawerPlugin().getHighlightColor());

        Rectangle r = RectangleMaskDrawerTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        r.x = (int)(zoom * r.x);
        r.y = (int)(zoom * r.y);
        r.width = (int)(zoom * r.width);
        r.height = (int)(zoom * r.height);
        if(fillToggle.isSelected()) {
            graphics2D.fillOval(x + r.x, y + r.y, r.width, r.height);
        }
        else {
            graphics2D.drawOval(x + r.x, y + r.y, r.width, r.height);
        }
    }

    @Subscribe
    public void onMouseMove(MouseMovedEvent event) {
        if(!isActive())
            return;
        getViewerPanel().getCanvas().repaint();
    }

    @Subscribe
    public void onMouseExited(MouseExitedEvent event) {
        if(!isActive())
            return;
        cancelDrawing();
    }
}
