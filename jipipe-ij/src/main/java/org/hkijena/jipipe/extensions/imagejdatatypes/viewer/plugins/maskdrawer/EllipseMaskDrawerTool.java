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

    private final JTextArea infoArea = UIUtils.makeReadonlyBorderlessTextArea("");
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
        updateInfo();
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
        formPanel.addToForm(infoArea, new JLabel(), null);
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
        updateInfo();
    }

    private void updateInfo() {
        if (referencePoint != null) {
            Point p0 = referencePoint;
            Point p1 = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
            Rectangle r = RectangleMaskDrawerTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
            if (p1 == null) {
                infoArea.setText(String.format("P1: %d, %d\n" +
                        "P2: -\n" +
                        "Width: -\n" +
                        "Height: -\n" +
                        "Area: -\n" +
                        "Circumference: -", p0.x, p0.y));
                return;
            }
            double a = r.getWidth() / 2.0;
            double b = r.getHeight() / 2.0;
            double h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
            infoArea.setText(String.format("P1: %d, %d\n" +
                            "P2: %d, %d\n" +
                            "Width: %d px\n" +
                            "Height: %d px\n" +
                            "Area: %f px²\n" +
                            "Circumference: %f px",
                    p0.x, p0.y,
                    p1.x, p1.y,
                    (int) r.getWidth(),
                    (int) r.getHeight(),
                    Math.PI * a * b,
                    Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h))))); // Rananujan approximation
        } else {
            infoArea.setText("P1: -\n" +
                    "P2: -\n" +
                    "Width: -\n" +
                    "Height: -\n" +
                    "Area: -\n" +
                    "Circumference: -");
        }
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
                updateInfo();
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

        Rectangle r = RectangleMaskDrawerTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        if (fillToggle.isSelected()) {
            processor.fillOval(r.x, r.y, r.width, r.height);
        } else {
            processor.drawOval(r.x, r.y, r.width, r.height);
        }
        getMaskDrawerPlugin().recalculateMaskPreview();
        postMaskChangedEvent();
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

        Rectangle r = RectangleMaskDrawerTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        r.x = (int) (zoom * r.x);
        r.y = (int) (zoom * r.y);
        r.width = (int) (zoom * r.width);
        r.height = (int) (zoom * r.height);
        if (fillToggle.isSelected()) {
            graphics2D.fillOval(x + r.x, y + r.y, r.width, r.height);
        } else {
            graphics2D.drawOval(x + r.x, y + r.y, r.width, r.height);
        }
    }

    @Subscribe
    public void onMouseMove(MouseMovedEvent event) {
        if (!isActive())
            return;
        getViewerPanel().getCanvas().repaint();
        updateInfo();
    }

    @Subscribe
    public void onMouseExited(MouseExitedEvent event) {
        if (!isActive())
            return;
        cancelDrawing();
    }
}
