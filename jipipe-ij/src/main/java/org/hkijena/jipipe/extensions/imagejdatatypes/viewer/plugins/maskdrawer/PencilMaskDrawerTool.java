package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ui.MouseClickedEvent;
import org.hkijena.jipipe.utils.ui.MouseDraggedEvent;
import org.hkijena.jipipe.utils.ui.MouseExitedEvent;
import org.hkijena.jipipe.utils.ui.MouseMovedEvent;
import org.hkijena.jipipe.utils.ui.MousePressedEvent;
import org.hkijena.jipipe.utils.ui.MouseReleasedEvent;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PencilMaskDrawerTool extends MaskDrawerTool {

    public static int DEFAULT_SETTING_PENCIL_SIZE_X = 12;
    public static int DEFAULT_SETTING_PENCIL_SIZE_Y = 12;

    private final Set<Point> interpolationPoints = new HashSet<>();
    private ImagePlus currentPencil;
    private BufferedImage currentPencilGhost;
    private JSpinner pencilSizeXSpinner;
    private JSpinner pencilSizeYSpinner;
    private JComboBox<PencilShape> pencilShapeSelection;
    private Point lastPencilPosition;
    private boolean isDrawing;

    public PencilMaskDrawerTool(MaskDrawerPlugin plugin) {
        super(plugin,
                "Pencil",
                "Allows to draw free-hand",
                UIUtils.getIconFromResources("actions/draw-brush.png"));
        getViewerPanel().getCanvas().getEventBus().register(this);
        initialize();
    }

    private void initialize() {
        SpinnerNumberModel pencilSizeXModel = new SpinnerNumberModel(DEFAULT_SETTING_PENCIL_SIZE_X, 1, Integer.MAX_VALUE, 1);
        pencilSizeXSpinner = new JSpinner(pencilSizeXModel);
        SpinnerNumberModel pencilSizeYModel = new SpinnerNumberModel(DEFAULT_SETTING_PENCIL_SIZE_Y, 1, Integer.MAX_VALUE, 1);
        pencilSizeYSpinner = new JSpinner(pencilSizeYModel);

        pencilSizeXModel.addChangeListener(e -> {
            DEFAULT_SETTING_PENCIL_SIZE_X = Math.max(1, pencilSizeXModel.getNumber().intValue());
            recalculatePencil();
        });
        pencilSizeYModel.addChangeListener(e -> {
            DEFAULT_SETTING_PENCIL_SIZE_Y = Math.max(1, pencilSizeYModel.getNumber().intValue());
            recalculatePencil();
        });

        pencilShapeSelection = new JComboBox<>(PencilShape.values());
        pencilShapeSelection.setSelectedItem(PencilShape.Ellipse);
        pencilShapeSelection.addActionListener(e -> recalculatePencil());
    }

    private void recalculatePencil() {
        final int pencilWidth = ((Number) pencilSizeXSpinner.getModel().getValue()).intValue();
        final int pencilHeight = ((Number) pencilSizeYSpinner.getModel().getValue()).intValue();
        if (currentPencil == null || currentPencil.getWidth() != pencilWidth || currentPencil.getHeight() != pencilHeight) {
            currentPencil = IJ.createImage("Pencil", pencilWidth, pencilHeight, 1, 8);
            currentPencilGhost = new BufferedImage(pencilWidth, pencilHeight, BufferedImage.TYPE_4BYTE_ABGR);
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
    public void createPalettePanel(FormPanel formPanel) {
        formPanel.addToForm(pencilShapeSelection, new JLabel("Pencil type"), null);
        formPanel.addToForm(pencilSizeXSpinner, new JLabel("Pencil width"), null);
        formPanel.addToForm(pencilSizeYSpinner, new JLabel("Pencil height"), null);
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
    public void activate() {
        getViewerPanel().getCanvas().setDragWithLeftMouse(false);
        getViewerPanel().getCanvas().setStandardCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        recalculatePencil();
        releasePencil();
    }

    @Override
    public void deactivate() {
        getViewerPanel().getCanvas().setDragWithLeftMouse(true);
    }

    @Subscribe
    public void onMouseMove(MouseMovedEvent event) {
        if (!isActive())
            return;
        if (isDrawing) {
            drawPencil();
        }
        getViewerPanel().getCanvas().repaint();
    }

    @Subscribe
    public void onMouseClick(MouseClickedEvent event) {
        if (!isActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            releasePencil();
            drawPencil();
            releasePencil();
        }
    }

    @Subscribe
    public void onMouseDrag(MouseDraggedEvent event) {
        if (!isActive())
            return;
        if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK) {
            drawPencil();
        } else {
            releasePencil();
        }
    }

    @Subscribe
    public void onMousePressed(MousePressedEvent event) {
        if (!isActive())
            return;
        if (SwingUtilities.isLeftMouseButton(event)) {
            isDrawing = true;
            drawPencil();
        }
    }

    @Subscribe
    public void onMouseReleased(MouseReleasedEvent event) {
        if (!isActive())
            return;
        releasePencil();
    }

    @Subscribe
    public void onMouseExited(MouseExitedEvent event) {
        if (!isActive())
            return;
        releasePencil();
    }

    private void releasePencil() {
        isDrawing = false;
        lastPencilPosition = null;
        postMaskChangedEvent();
    }

    @Override
    public void postprocessDraw(Graphics2D graphics2D, int x, int y, int w, int h) {
        Point mousePosition = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
        final double zoom = getViewerPanel().getCanvas().getZoom();
        AffineTransform transform = new AffineTransform();
        transform.scale(zoom, zoom);
        BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        if (currentPencil == null || currentPencilGhost == null || mousePosition == null)
            return;
        final int pencilWidth = currentPencil.getWidth();
        final int pencilHeight = currentPencil.getHeight();
        final int displayedPencilWidth = (int) (zoom * pencilWidth);
        final int displayedPencilHeight = (int) (zoom * pencilHeight);
        int displayedPencilX = (int) (x + zoom * mousePosition.x - displayedPencilWidth / 2);
        int displayedPencilY = (int) (y + zoom * mousePosition.y - displayedPencilHeight / 2);

        graphics2D.drawImage(currentPencilGhost, op, displayedPencilX, displayedPencilY);
    }

    /**
     * Draws the pencil tool at the current mouse position
     */
    private void drawPencil() {
        // Copy the pencil into the mask buffer
        Point mousePosition = getViewerPanel().getCanvas().getMouseModelPixelCoordinate(false);
        if (mousePosition == null) {
            releasePencil();
            return;
        }
        final int pencilWidth = currentPencil.getWidth();
        final int pencilHeight = currentPencil.getHeight();
        final int centerX = mousePosition.x - pencilWidth / 2;
        final int centerY = mousePosition.y - pencilHeight / 2;
        Point center = new Point(centerX, centerY);
        int blitter = getMaskDrawerPlugin().getCurrentColor() == MaskDrawerPlugin.MaskColor.Foreground ? Blitter.ADD : Blitter.SUBTRACT;
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
