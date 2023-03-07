package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.MouseClickedEvent;
import org.hkijena.jipipe.utils.ui.MouseExitedEvent;
import org.hkijena.jipipe.utils.ui.MouseMovedEvent;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Polygon drawing
 * Allows left-click canvas dragging
 */
public class PolygonMaskDrawer2DTool extends MaskDrawer2DTool {

    public static boolean DEFAULT_SETTING_CLOSE = true;
    public static boolean DEFAULT_SETTING_FILL = true;

    private final JTextArea infoArea = UIUtils.makeReadonlyBorderlessTextArea("");
    private List<Point> referencePoints = new ArrayList<>();
    private JCheckBox closeToggle;
    private JCheckBox fillToggle;

    public PolygonMaskDrawer2DTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "Polygon",
                "Draws a polygon.\n" +
                        "Add points with left click.\n" +
                        "Remove points with right click. Double right-click to cancel drawing.\n" +
                        "Draw by double-clicking.",
                UIUtils.getIconFromResources("actions/draw-polyline.png"));
        getViewerPanel2D().getCanvas().getEventBus().register(this);
        initialize();
        updateInfo();
    }

    private void initialize() {
        closeToggle = new JCheckBox("Close polygon", DEFAULT_SETTING_CLOSE);
        closeToggle.addActionListener(e -> {
            DEFAULT_SETTING_CLOSE = closeToggle.isSelected();
        });
        fillToggle = new JCheckBox("Fill", DEFAULT_SETTING_FILL);
        fillToggle.addActionListener(e -> {
            DEFAULT_SETTING_FILL = fillToggle.isSelected();
        });
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        formPanel.addToForm(closeToggle, new JLabel(), null);
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
        referencePoints.clear();
        getViewerPanel2D().getCanvas().repaint(50);
        updateInfo();
    }

    private void updateInfo() {
        if (referencePoints.isEmpty()) {
            infoArea.setText("Length: -\n" +
                    "Length (+ mouse): -");
        } else {
            double length = 0;
            for (int i = 1; i < referencePoints.size(); i++) {
                Point p0 = referencePoints.get(i - 1);
                Point p1 = referencePoints.get(i);
                length += p0.distance(p1);
            }
            double lengthPlusMouse = 0;
            Point mouse = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
            if (mouse != null) {
                lengthPlusMouse = length + referencePoints.get(referencePoints.size() - 1).distance(mouse);
            }
            Calibration calibration = getViewerPanel().getImage().getCalibration();
            if(calibration != null && calibration.scaled() && calibration.pixelWidth == calibration.pixelHeight) {
                infoArea.setText(String.format("Length: %f px (%f %s)\n" +
                                "Length (+ mouse): %f px (%f %s)",
                        length,
                        length * calibration.pixelWidth,
                        calibration.getXUnit(),
                        lengthPlusMouse,
                        lengthPlusMouse * calibration.pixelWidth,
                        calibration.getXUnit()));
            }
            else {
                infoArea.setText(String.format("Length: %f px\n" +
                                "Length (+ mouse): %f px",
                        length,
                        lengthPlusMouse));
            }
        }
    }

    @Subscribe
    public void onMouseClick(MouseClickedEvent event) {
        if (!toolIsActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            Point point = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
            if (point == null) {
                cancelDrawing();
                return;
            }

            referencePoints.add(point);
            updateInfo();
            if (event.getClickCount() > 1) {
                applyDrawing();
                cancelDrawing();
            }
        } else if (SwingUtilities.isRightMouseButton(event)) {
            if (event.getClickCount() > 1) {
                cancelDrawing();
            } else {
                if (!referencePoints.isEmpty()) {
                    // Remove last reference point
                    referencePoints.remove(referencePoints.size() - 1);
                    getViewerPanel2D().getCanvas().repaint(50);
                    updateInfo();
                }
            }
        }
    }

    private void applyDrawing() {
        if (referencePoints.isEmpty())
            return;

        ImageProcessor processor = getMaskDrawerPlugin().getCurrentMaskSlice();
        processor.setValue(getMaskDrawerPlugin().getCurrentColor().getValue());

        int nPoints = referencePoints.size();
        if (closeToggle.isSelected()) {
            ++nPoints;
        }
        int[] xCoordinates = new int[nPoints];
        int[] yCoordinates = new int[nPoints];

        for (int i = 0; i < referencePoints.size(); i++) {
            xCoordinates[i] = referencePoints.get(i).x;
            yCoordinates[i] = referencePoints.get(i).y;
        }

        if (closeToggle.isSelected()) {
            int i = nPoints - 1;
            xCoordinates[i] = referencePoints.get(0).x;
            yCoordinates[i] = referencePoints.get(0).y;
        }

        PolygonRoi roi = new PolygonRoi(xCoordinates, yCoordinates, nPoints, fillToggle.isSelected() ? Roi.POLYGON : Roi.POLYLINE);
        if (fillToggle.isSelected()) {
            processor.fill(roi);
        } else {
            processor.draw(roi);
        }
        getMaskDrawerPlugin().recalculateMaskPreview();
        postMaskChangedEvent();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        if (referencePoints.isEmpty())
            return;
        final int renderX = renderArea.x;
        final int renderY = renderArea.y;
        final double zoom = getViewerPanel2D().getCanvas().getZoom();
        Point point = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
        graphics2D.setColor(getMaskDrawerPlugin().getHighlightColor());

        int nPoints = referencePoints.size() + 1;
        int[] xCoordinates = new int[nPoints];
        int[] yCoordinates = new int[nPoints];

        for (int i = 0; i < referencePoints.size(); i++) {
            xCoordinates[i] = renderX + (int) (zoom * referencePoints.get(i).x);
            yCoordinates[i] = renderY + (int) (zoom * referencePoints.get(i).y);
        }

        if (point != null) {
            xCoordinates[xCoordinates.length - 1] = (int) (renderX + zoom * point.x);
            yCoordinates[yCoordinates.length - 1] = (int) (renderY + zoom * point.y);
        }

        Polygon polygon = new Polygon(xCoordinates, yCoordinates, nPoints);

        graphics2D.draw(polygon);
        if (fillToggle.isSelected()) {
            graphics2D.fill(polygon);
        }
    }

    @Subscribe
    public void onMouseMove(MouseMovedEvent event) {
        if (!toolIsActive())
            return;
        getViewerPanel2D().getCanvas().repaint(50);
        updateInfo();
    }

    @Subscribe
    public void onMouseExited(MouseExitedEvent event) {
        if (!toolIsActive())
            return;
        cancelDrawing();
    }
}
