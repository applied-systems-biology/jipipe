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
 * Ellipse drawing
 * Allows left-click canvas dragging
 */
public class EllipseMaskDrawer2DTool extends MaskDrawer2DTool implements MouseClickedEventListener, MouseExitedEventListener, MouseMovedEventListener {

    public static boolean DEFAULT_SETTING_START_FROM_CENTER = false;
    public static boolean DEFAULT_SETTING_SQUARE = false;
    public static boolean DEFAULT_SETTING_FILL = true;

    private final JTextArea infoArea = UIUtils.makeReadonlyBorderlessTextArea("");
    private Point referencePoint;
    private JCheckBox startFromCenterToggle;
    private JCheckBox squareToggle;
    private JCheckBox fillToggle;

    public EllipseMaskDrawer2DTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "Ellipse",
                "Draws an ellipse between two points",
                UIUtils.getIconFromResources("actions/draw-ellipse-whole.png"));
        ImageViewerPanelCanvas2D canvas = getViewerPanel2D().getCanvas();
        canvas.getMouseClickedEventEmitter().subscribe(this);
        canvas.getMouseExitedEventEmitter().subscribe(this);
        canvas.getMouseMovedEventEmitter().subscribe(this);
        initialize();
        updateInfo();
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
    public void onToolActivate(ImageViewerPanelCanvas2D canvas) {

    }

    @Override
    public void onToolDeactivate(ImageViewerPanelCanvas2D canvas) {
        cancelDrawing();
    }

    @Override
    public Cursor getToolCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    private void cancelDrawing() {
        referencePoint = null;
        getViewerPanel2D().getCanvas().repaint(50);
        updateInfo();
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
                    double a = r.getWidth() / 2.0;
                    double b = r.getHeight() / 2.0;
                    double h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
                    double a2 = (r.getWidth() * calibration.pixelWidth) / 2.0;
                    double b2 = (r.getHeight() * calibration.pixelHeight) / 2.0;
                    double h2 = Math.pow(a2 - b2, 2) / Math.pow(a2 + b2, 2);
                    infoArea.setText(String.format("P1: %d, %d\n" +
                                    "P2: %d, %d\n" +
                                    "Width: %d px (%f %s)\n" +
                                    "Height: %d px (%f %s)\n" +
                                    "Area: %f px² (%f %s²)\n" +
                                    "Circumference: %f px (%f %s)",
                            p0.x, p0.y,
                            p1.x, p1.y,
                            (int) r.getWidth(),
                            r.getWidth() * calibration.pixelWidth,
                            calibration.getXUnit(),
                            (int) r.getHeight(),
                            r.getHeight() * calibration.pixelHeight,
                            calibration.getYUnit(),
                            Math.PI * a * b,
                            Math.PI * a * b * calibration.pixelWidth * calibration.pixelHeight,
                            calibration.getXUnit(),
                            Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h))),
                            Math.PI * (a2 + b2) * (1 + (3 * h2) / (10 + Math.sqrt(4 - 3 * h2))),
                            calibration.getXUnit())); // Ramanujan approximation
                } else {
                    double a = r.getWidth() / 2.0;
                    double b = r.getHeight() / 2.0;
                    double h = Math.pow(a - b, 2) / Math.pow(a + b, 2);
                    infoArea.setText(String.format("P1: %d, %d\n" +
                                    "P2: %d, %d\n" +
                                    "Width: %d px (%f %s)\n" +
                                    "Height: %d px (%f %s)\n" +
                                    "Area: %f px²\n" +
                                    "Circumference: %f px",
                            p0.x, p0.y,
                            p1.x, p1.y,
                            (int) r.getWidth(),
                            r.getWidth() * calibration.pixelWidth,
                            calibration.getXUnit(),
                            (int) r.getHeight(),
                            r.getHeight() * calibration.pixelHeight,
                            calibration.getYUnit(),
                            Math.PI * a * b,
                            Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h))))); // Ramanujan approximation
                }
            } else {
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
                        Math.PI * (a + b) * (1 + (3 * h) / (10 + Math.sqrt(4 - 3 * h))))); // Ramanujan approximation
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

        Rectangle r = RectangleMaskDrawer2DTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        if (fillToggle.isSelected()) {
            processor.fillOval(r.x, r.y, r.width, r.height);
        } else {
            processor.drawOval(r.x, r.y, r.width, r.height);
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

        Rectangle r = RectangleMaskDrawer2DTool.getDrawnArea(p0, p1, startFromCenterToggle.isSelected(), squareToggle.isSelected());
        r.x = (int) (zoom * r.x);
        r.y = (int) (zoom * r.y);
        r.width = (int) (zoom * r.width);
        r.height = (int) (zoom * r.height);
        if (fillToggle.isSelected()) {
            graphics2D.fillOval(renderX + r.x, renderY + r.y, r.width, r.height);
        } else {
            graphics2D.drawOval(renderX + r.x, renderY + r.y, r.width, r.height);
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
}
