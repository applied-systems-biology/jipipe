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
 * Rectangle drawing
 * Allows left-click canvas dragging
 */
public class RectangleMaskDrawerTool extends MaskDrawerTool {

    private Point referencePoint;
    private JCheckBox startFromCenterToggle;
    private JCheckBox squareToggle;
    private JCheckBox fillToggle;

    public RectangleMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin,
                "Rectangle",
                "Draws a rectangle between two points",
                UIUtils.getIconFromResources("actions/draw-rectangle.png"));
        getViewerPanel().getCanvas().getEventBus().register(this);
        initialize();
    }

    public static Rectangle getDrawnArea(Point p0, Point p1, boolean startFromCenter, boolean square) {
        if (startFromCenter) {
            int rw = 2 * Math.abs(p0.x - p1.x);
            int rh = 2 * Math.abs(p0.y - p1.y);
            int rx = p0.x - rw / 2;
            int ry = p0.y - rh / 2;
            if (square) {
                if (rw < rh) {
                    int d = rh - rw;
                    rh = rw;
                    ry += d / 2;
                } else if (rh < rw) {
                    int d = rw - rh;
                    rw = rh;
                    rx += d / 2;
                }
            }
            return new Rectangle(rx, ry, rw, rh);
        } else {
            int rx = Math.min(p0.x, p1.x);
            int ry = Math.min(p0.y, p1.y);
            int rw = Math.abs(p0.x - p1.x);
            int rh = Math.abs(p0.y - p1.y);
            if (square) {
                if (rw < rh) {
                    int d = rh - rw;
                    rh = rw;
                    if (p0.y > p1.y) {
                        ry += d;
                    }
                } else if (rh < rw) {
                    int d = rw - rh;
                    rw = rh;
                    if (p0.x > p1.x) {
                        rx += d;
                    }
                }
            }
            return new Rectangle(rx, ry, rw, rh);
        }
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
        if (!isActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            Point point = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
            if (point == null) {
                cancelDrawing();
                return;
            }

            if (referencePoint == null) {
                referencePoint = point;
            } else {
                applyDrawing(referencePoint, point);
                cancelDrawing();
            }
        } else if (SwingUtilities.isRightMouseButton(event)) {
            cancelDrawing();
        }
    }

    private void applyDrawing(Point p0, Point p1) {
        ImageProcessor processor = getMaskDrawerPlugin().getCurrentMaskSlice();
        processor.setValue(getMaskDrawerPlugin().getCurrentColor().getValue());

        Rectangle r = getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        if (fillToggle.isSelected()) {
            processor.fillRect(r.x, r.y, r.width, r.height);
        } else {
            processor.drawRect(r.x, r.y, r.width, r.height);
        }
        getMaskDrawerPlugin().recalculateMaskPreview();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        if (referencePoint == null)
            return;
        Point p0 = referencePoint;
        Point p1 = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
        if (p1 == null)
            return;
        final double zoom = getViewerPanel().getCanvas().getZoom();
        graphics2D.setColor(getMaskDrawerPlugin().getHighlightColor());

        Rectangle r = getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        r.x = (int) (zoom * r.x);
        r.y = (int) (zoom * r.y);
        r.width = (int) (zoom * r.width);
        r.height = (int) (zoom * r.height);
        if (fillToggle.isSelected()) {
            graphics2D.fillRect(x + r.x, y + r.y, r.width, r.height);
        } else {
            graphics2D.drawRect(x + r.x, y + r.y, r.width, r.height);
        }
    }

    @Subscribe
    public void onMouseMove(MouseMovedEvent event) {
        if (!isActive())
            return;
        getViewerPanel().getCanvas().repaint();
    }

    @Subscribe
    public void onMouseExited(MouseExitedEvent event) {
        if (!isActive())
            return;
        cancelDrawing();
    }
}
