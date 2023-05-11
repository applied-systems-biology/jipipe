package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.events.*;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Rectangle drawing
 * Allows left-click canvas dragging
 */
public class RectangleMaskDrawer2DTool extends MaskDrawer2DTool implements MouseClickedEventListener, MouseExitedEventListener, MouseMovedEventListener {

    public static boolean DEFAULT_SETTING_START_FROM_CENTER = false;
    public static boolean DEFAULT_SETTING_SQUARE = false;
    public static boolean DEFAULT_SETTING_FILL = true;

    private final JTextArea infoArea = UIUtils.makeReadonlyBorderlessTextArea("");
    private Point referencePoint;
    private JCheckBox startFromCenterToggle;
    private JCheckBox squareToggle;
    private JCheckBox fillToggle;

    public RectangleMaskDrawer2DTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "Rectangle",
                "Draws a rectangle between two points",
                UIUtils.getIconFromResources("actions/draw-rectangle.png"));
        ImageViewerPanelCanvas2D canvas = getViewerPanel2D().getCanvas();
        canvas.getMouseClickedEventEmitter().subscribe(this);
        canvas.getMouseExitedEventEmitter().subscribe(this);
        canvas.getMouseMovedEventEmitter().subscribe(this);
        initialize();
        updateInfo();
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
        startFromCenterToggle = new JCheckBox("Start from center", DEFAULT_SETTING_START_FROM_CENTER);
        startFromCenterToggle.addActionListener(e -> {
            DEFAULT_SETTING_START_FROM_CENTER = startFromCenterToggle.isSelected();
        });
        squareToggle = new JCheckBox("Draw square", DEFAULT_SETTING_SQUARE);
        squareToggle.addActionListener(e -> {
            DEFAULT_SETTING_SQUARE = squareToggle.isSelected();
        });
        fillToggle = new JCheckBox("Fill", DEFAULT_SETTING_FILL);
        fillToggle.addActionListener(e -> {
            DEFAULT_SETTING_FILL = fillToggle.isSelected();
        });
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        formPanel.addToForm(startFromCenterToggle, new JLabel(), null);
        formPanel.addToForm(squareToggle, new JLabel(), null);
        formPanel.addToForm(fillToggle, new JLabel(), null);
        formPanel.addToForm(infoArea, new JLabel(), null);
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
        cancelDrawing();
    }

    private void cancelDrawing() {
        referencePoint = null;
        getViewerPanel2D().getCanvas().repaint(50);
        updateInfo();
    }

    @Override
    public void onComponentMouseClicked(MouseClickedEvent event) {
        if (!toolIsActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            Point point = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
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

        Rectangle r = getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        if (fillToggle.isSelected()) {
            processor.fillRect(r.x, r.y, r.width, r.height);
        } else {
            processor.drawRect(r.x, r.y, r.width, r.height);
        }
        getMaskDrawerPlugin().recalculateMaskPreview();
        postMaskChangedEvent();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (referencePoint == null)
            return;
        final int renderX = renderArea.x;
        final int renderY = renderArea.y;
        Point p0 = referencePoint;
        Point p1 = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
        if (p1 == null)
            return;
        final double zoom = getViewerPanel2D().getCanvas().getZoom();
        graphics2D.setColor(getMaskDrawerPlugin().getHighlightColor());

        Rectangle r = getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        r.x = (int) (zoom * r.x);
        r.y = (int) (zoom * r.y);
        r.width = (int) (zoom * r.width);
        r.height = (int) (zoom * r.height);
        if (fillToggle.isSelected()) {
            graphics2D.fillRect(renderX + r.x, renderY + r.y, r.width, r.height);
        } else {
            graphics2D.drawRect(renderX + r.x, renderY + r.y, r.width, r.height);
        }
    }

    @Override
    public void onComponentMouseMoved(MouseMovedEvent event) {
        if (!toolIsActive())
            return;
        getViewerPanel2D().getCanvas().repaint(50);
        updateInfo();
    }

    @Override
    public void onComponentMouseExited(MouseExitedEvent event) {
        if (!toolIsActive())
            return;
        cancelDrawing();
    }

    private void updateInfo() {
        if (referencePoint != null) {
            Point p0 = referencePoint;
            Point p1 = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
            Rectangle r = RectangleMaskDrawer2DTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
            if (p1 == null) {
                infoArea.setText(String.format("P1: %d, %d\n" +
                        "P2: -\n" +
                        "Width: -\n" +
                        "Height: -\n" +
                        "Area: -\n" +
                        "Circumference: -", p0.x, p0.y));
                return;
            }
            Calibration calibration = getViewerPanel().getImagePlus().getCalibration();
            if (calibration != null && calibration.scaled()) {
                if (calibration.pixelWidth == calibration.pixelHeight && Objects.equals(calibration.getXUnit(), calibration.getYUnit())) {
                    infoArea.setText(String.format("P1: %d, %d\n" +
                                    "P2: %d, %d\n" +
                                    "Width: %d px (%f %s)\n" +
                                    "Height: %d px (%f %s)\n" +
                                    "Area: %d px² (%f %s²)\n" +
                                    "Circumference: %d px (%f %s)",
                            p0.x, p0.y,
                            p1.x, p1.y,
                            (int) r.getWidth(),
                            r.getWidth() * calibration.pixelWidth,
                            calibration.getXUnit(),
                            (int) r.getHeight(),
                            r.getHeight() * calibration.pixelHeight,
                            calibration.getYUnit(),
                            (int) (r.getWidth() * r.getHeight()),
                            (r.getWidth() * calibration.pixelWidth * r.getHeight() * calibration.pixelHeight),
                            calibration.getXUnit(),
                            (int) (2 * r.getWidth() + 2 * r.getHeight()),
                            (2 * r.getWidth() * calibration.pixelWidth + 2 * r.getHeight() * calibration.pixelHeight),
                            calibration.getXUnit()));
                } else {
                    infoArea.setText(String.format("P1: %d, %d\n" +
                                    "P2: %d, %d\n" +
                                    "Width: %d px (%f %s)\n" +
                                    "Height: %d px (%f %s)\n" +
                                    "Area: %d px²\n" +
                                    "Circumference: %d px",
                            p0.x, p0.y,
                            p1.x, p1.y,
                            (int) r.getWidth(),
                            r.getWidth() * calibration.pixelWidth,
                            calibration.getXUnit(),
                            (int) r.getHeight(),
                            r.getHeight() * calibration.pixelHeight,
                            calibration.getYUnit(),
                            (int) (r.getWidth() * r.getHeight()),
                            (int) (2 * r.getWidth() + 2 * r.getHeight())));
                }
            } else {
                infoArea.setText(String.format("P1: %d, %d\n" +
                                "P2: %d, %d\n" +
                                "Width: %d px\n" +
                                "Height: %d px\n" +
                                "Area: %d px²\n" +
                                "Circumference: %d px",
                        p0.x, p0.y,
                        p1.x, p1.y,
                        (int) r.getWidth(),
                        (int) r.getHeight(),
                        (int) (r.getWidth() * r.getHeight()),
                        (int) (2 * r.getWidth() + 2 * r.getHeight())));
            }
        } else {
            infoArea.setText("P1: -\n" +
                    "P2: -\n" +
                    "Width: -\n" +
                    "Height: -\n" +
                    "Area: -\n" +
                    "Circumference: -");
        }
    }
}
