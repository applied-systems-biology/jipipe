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

package org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.maskdrawer;

import ij.IJ;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d.ImageViewerPanelCanvas2D;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.events.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PencilMaskDrawer2DTool extends MaskDrawer2DTool implements MouseClickedEventListener, MouseExitedEventListener, MouseMovedEventListener,
        MouseDraggedEventListener, MousePressedEventListener, MouseReleasedEventListener {

    public static int DEFAULT_SETTING_PENCIL_SIZE_X = 12;
    public static int DEFAULT_SETTING_PENCIL_SIZE_Y = 12;
    public static boolean DEFAULT_SETTING_PENCIL_LINK_Y = true;

    private final Set<Point> interpolationPoints = new HashSet<>();
    private ImagePlus currentPencil;
    private BufferedImage currentPencilGhost;
    private JSpinner pencilSizeXSpinner;
    private JSpinner pencilSizeYSpinner;
    private JToggleButton pencilSizeYLinkToggle;
    private JComboBox<PencilShape> pencilShapeSelection;
    private Point lastPencilPosition;
    private boolean isDrawing;

    public PencilMaskDrawer2DTool(MaskDrawerPlugin2D plugin) {
        super(plugin,
                "Pencil",
                "Allows to draw free-hand",
                UIUtils.getIconFromResources("actions/draw-brush.png"));
        ImageViewerPanelCanvas2D canvas = getViewerPanel2D().getCanvas();
        canvas.getMouseClickedEventEmitter().subscribe(this);
        canvas.getMouseExitedEventEmitter().subscribe(this);
        canvas.getMouseMovedEventEmitter().subscribe(this);
        canvas.getMouseDraggedEventEmitter().subscribe(this);
        canvas.getMousePressedEventEmitter().subscribe(this);
        canvas.getMouseReleasedEventEmitter().subscribe(this);
        initialize();
    }

    private void initialize() {
        SpinnerNumberModel pencilSizeXModel = new SpinnerNumberModel(DEFAULT_SETTING_PENCIL_SIZE_X, 1, Integer.MAX_VALUE, 1);
        pencilSizeXSpinner = new JSpinner(pencilSizeXModel);
        SpinnerNumberModel pencilSizeYModel = new SpinnerNumberModel(DEFAULT_SETTING_PENCIL_SIZE_Y, 1, Integer.MAX_VALUE, 1);
        pencilSizeYSpinner = new JSpinner(pencilSizeYModel);
        pencilSizeYLinkToggle = new JToggleButton(UIUtils.getIconFromResources("actions/edit-link.png"), DEFAULT_SETTING_PENCIL_LINK_Y);
        pencilSizeYLinkToggle.setToolTipText("Keep the Y size the same as the X size");
        UIUtils.makeButtonFlat25x25(pencilSizeYLinkToggle);

        pencilSizeXModel.addChangeListener(e -> {
            DEFAULT_SETTING_PENCIL_SIZE_X = Math.max(1, pencilSizeXModel.getNumber().intValue());
            if (pencilSizeYLinkToggle.isSelected()) {
                pencilSizeYModel.setValue(pencilSizeXModel.getNumber());
            }
            recalculatePencil();
        });
        pencilSizeYModel.addChangeListener(e -> {
            DEFAULT_SETTING_PENCIL_SIZE_Y = Math.max(1, pencilSizeYModel.getNumber().intValue());
            recalculatePencil();
        });
        pencilSizeYLinkToggle.addActionListener(e -> {
            DEFAULT_SETTING_PENCIL_LINK_Y = pencilSizeYLinkToggle.isSelected();
            if (pencilSizeYLinkToggle.isSelected()) {
                pencilSizeYModel.setValue(pencilSizeXModel.getNumber());
            }
        });

        pencilShapeSelection = new JComboBox<>(PencilShape.values());
        pencilShapeSelection.setSelectedItem(PencilShape.Ellipse);
        pencilShapeSelection.addActionListener(e -> recalculatePencil());

        recalculatePencil();
    }

    private void recalculatePencil() {
        final int pencilWidth = ((Number) pencilSizeXSpinner.getModel().getValue()).intValue();
        final int pencilHeight = ((Number) pencilSizeYSpinner.getModel().getValue()).intValue();
        if (currentPencil == null || currentPencil.getWidth() != pencilWidth || currentPencil.getHeight() != pencilHeight) {
            currentPencil = IJ.createImage("Pencil", pencilWidth, pencilHeight, 1, 8);
            currentPencilGhost = new BufferedImage(pencilWidth, pencilHeight, BufferedImage.TYPE_INT_ARGB);
        }
        ByteProcessor processor = (ByteProcessor) currentPencil.getProcessor();

        switch ((PencilShape) pencilShapeSelection.getSelectedItem()) {
            case Rectangle: {
                byte[] pixels = (byte[]) processor.getPixels();
                Arrays.fill(pixels, (byte) 255);
            }
            break;
            case Ellipse: {
                byte[] pixels = (byte[]) processor.getPixels();
                Arrays.fill(pixels, (byte) 0);
                processor.setValue(255);
                processor.fillOval(0, 0, pencilWidth, pencilHeight);
            }
        }

        // Recalculate pencil image
        recalculatePencilGhost();
    }

    @Override
    public void buildPanel(JIPipeDesktopFormPanel formPanel) {
        formPanel.addToForm(pencilShapeSelection, new JLabel("Pencil type"), null);
        formPanel.addToForm(pencilSizeXSpinner, new JLabel("Pencil width"), null);
        JPanel sizeYPanel = new JPanel(new BorderLayout());
        sizeYPanel.add(pencilSizeYLinkToggle, BorderLayout.WEST);
        sizeYPanel.add(pencilSizeYSpinner, BorderLayout.CENTER);
        formPanel.addToForm(sizeYPanel, new JLabel("Pencil height"), null);
    }

    private void recalculatePencilGhost() {
        ImageJUtils.maskToBufferedImage(currentPencil.getProcessor(),
                currentPencilGhost,
                getMaskDrawerPlugin().getHighlightColor(),
                ColorUtils.WHITE_TRANSPARENT);
    }

    @Override
    public void onHighlightColorChanged() {
        recalculatePencilGhost();
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
    }

    @Override
    public void onComponentMouseMoved(MouseMovedEvent event) {
        if (!toolIsActive())
            return;
        if (isDrawing) {
            drawPencil();
        }
        getViewerPanel2D().getCanvas().repaint(50);
    }

    @Override
    public void onComponentMouseClicked(MouseClickedEvent event) {
        if (!toolIsActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            releasePencil();
            drawPencil();
            releasePencil();
        }
    }

    @Override
    public void onComponentMouseDragged(MouseDraggedEvent event) {
        if (!toolIsActive())
            return;
        if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
            drawPencil();
        } else {
            releasePencil();
        }
    }

    @Override
    public void onComponentMousePressed(MousePressedEvent event) {
        if (!toolIsActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            isDrawing = true;
            drawPencil();
        }
    }

    @Override
    public void onComponentMouseReleased(MouseReleasedEvent event) {
        if (!toolIsActive())
            return;
        releasePencil();
    }

    @Override
    public void onComponentMouseExited(MouseExitedEvent event) {
        if (!toolIsActive())
            return;
        releasePencil();
    }

    private void releasePencil() {
        isDrawing = false;
        lastPencilPosition = null;
        postMaskChangedEvent();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, Rectangle renderArea, ImageSliceIndex sliceIndex) {
        final int renderX = renderArea.x;
        final int renderY = renderArea.y;
        Point mousePosition = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
        final double zoom = getViewerPanel2D().getCanvas().getZoom();
        AffineTransform transform = new AffineTransform();
        transform.scale(zoom, zoom);
        BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        if (currentPencil == null || currentPencilGhost == null || mousePosition == null)
            return;
        final int pencilWidth = currentPencil.getWidth();
        final int pencilHeight = currentPencil.getHeight();
        final int displayedPencilWidth = (int) (zoom * pencilWidth);
        final int displayedPencilHeight = (int) (zoom * pencilHeight);
        int displayedPencilX = (int) (renderX + zoom * mousePosition.x - displayedPencilWidth / 2);
        int displayedPencilY = (int) (renderY + zoom * mousePosition.y - displayedPencilHeight / 2);

        graphics2D.drawImage(currentPencilGhost, op, displayedPencilX, displayedPencilY);
    }

    /**
     * Draws the pencil tool at the current mouse position
     */
    private void drawPencil() {
        // Copy the pencil into the mask buffer
        Point mousePosition = getViewerPanel2D().getCanvas().getMouseModelPixelCoordinate(null, false);
        if (mousePosition == null) {
            releasePencil();
            return;
        }
        final int pencilWidth = currentPencil.getWidth();
        final int pencilHeight = currentPencil.getHeight();
        final int centerX = mousePosition.x - pencilWidth / 2;
        final int centerY = mousePosition.y - pencilHeight / 2;
        Point center = new Point(centerX, centerY);
        int blitter = getMaskDrawerPlugin().getCurrentColor() == MaskDrawerPlugin2D.MaskColor.Foreground ? Blitter.ADD : Blitter.SUBTRACT;
        if (lastPencilPosition != null) {
            // We need to interpolate, so the stroke is connected
            double distance = center.distance(lastPencilPosition);
            if (distance > 1) {
                double vecX = (centerX - lastPencilPosition.x) / distance;
                double vecY = (centerY - lastPencilPosition.y) / distance;
                double sx = lastPencilPosition.x;
                double sy = lastPencilPosition.y;
                interpolationPoints.clear();
                while (distance > 1) {
                    sx += vecX;
                    sy += vecY;
                    Point interpolationCenter = new Point((int) sx, (int) sy);
                    if (!interpolationPoints.contains(interpolationCenter)) {
                        getMaskDrawerPlugin().getCurrentMaskSlice().copyBits(currentPencil.getProcessor(),
                                interpolationCenter.x,
                                interpolationCenter.y,
                                blitter);
                        interpolationPoints.add(interpolationCenter);
                    }
                    distance -= 1;
                }
            }
        }
        getMaskDrawerPlugin().getCurrentMaskSlice().copyBits(currentPencil.getProcessor(),
                mousePosition.x - pencilWidth / 2,
                mousePosition.y - pencilHeight / 2,
                blitter);
        lastPencilPosition = center;
        getMaskDrawerPlugin().recalculateMaskPreview();
    }

    public enum PencilShape {
        Rectangle,
        Ellipse
    }
}
