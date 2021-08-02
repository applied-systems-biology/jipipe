package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ui.MouseClickedEvent;
import org.hkijena.jipipe.utils.ui.MouseExitedEvent;
import org.hkijena.jipipe.utils.ui.MouseMovedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * Polygon drawing
 * Allows left-click canvas dragging
 */
public class PolygonMaskDrawerTool extends MaskDrawerTool {

    public static boolean DEFAULT_SETTING_CLOSE = true;
    public static boolean DEFAULT_SETTING_FILL = true;

    private final JTextArea infoArea = UIUtils.makeReadonlyBorderlessTextArea("");
    private List<Point> referencePoints = new ArrayList<>();
    private JCheckBox closeToggle;
    private JCheckBox fillToggle;

    public PolygonMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin,
                "Polygon",
                "Draws a polygon.\n" +
                        "Add points with left click.\n" +
                        "Remove points with right click. Double right-click to cancel drawing.\n" +
                        "Draw by double-clicking.",
                UIUtils.getIconFromResources("actions/draw-polyline.png"));
        getViewerPanel().getCanvas().getEventBus().register(this);
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
    public void createPalettePanel(FormPanel formPanel) {
        formPanel.addToForm(closeToggle, new JLabel(), null);
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
        referencePoints.clear();
        getViewerPanel().getCanvas().repaint();
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
            Point mouse = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
            if (mouse != null) {
                lengthPlusMouse = length + referencePoints.get(referencePoints.size() - 1).distance(mouse);
            }
            infoArea.setText(String.format("Length: %f px\n" +
                            "Length (+ mouse): %f px",
                    length,
                    lengthPlusMouse));
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
                    getViewerPanel().getCanvas().repaint();
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
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        if (referencePoints.isEmpty())
            return;
        final double zoom = getViewerPanel().getCanvas().getZoom();
        Point point = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
        graphics2D.setColor(getMaskDrawerPlugin().getHighlightColor());

        int nPoints = referencePoints.size() + 1;
        int[] xCoordinates = new int[nPoints];
        int[] yCoordinates = new int[nPoints];

        for (int i = 0; i < referencePoints.size(); i++) {
            xCoordinates[i] = x + (int) (zoom * referencePoints.get(i).x);
            yCoordinates[i] = y + (int) (zoom * referencePoints.get(i).y);
        }

        if (point != null) {
            xCoordinates[xCoordinates.length - 1] = (int) (x + zoom * point.x);
            yCoordinates[yCoordinates.length - 1] = (int) (y + zoom * point.y);
        }

        Polygon polygon = new Polygon(xCoordinates, yCoordinates, nPoints);

        graphics2D.draw(polygon);
        if (fillToggle.isSelected()) {
            graphics2D.fill(polygon);
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
