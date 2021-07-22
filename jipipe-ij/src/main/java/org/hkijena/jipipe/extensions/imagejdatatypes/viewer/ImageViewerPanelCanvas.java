/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.viewer;

import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.utils.ui.MouseClickedEvent;
import org.hkijena.jipipe.utils.ui.MouseDraggedEvent;
import org.hkijena.jipipe.utils.ui.MouseEnteredEvent;
import org.hkijena.jipipe.utils.ui.MouseExitedEvent;
import org.hkijena.jipipe.utils.ui.MouseMovedEvent;
import org.hkijena.jipipe.utils.ui.MousePressedEvent;
import org.hkijena.jipipe.utils.ui.MouseReleasedEvent;

import javax.swing.*;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

public class ImageViewerPanelCanvas extends JPanel implements MouseListener, MouseMotionListener {
    private final ImageViewerPanel imageViewerPanel;
    private final EventBus eventBus = new EventBus();
    private BufferedImage image;
    private double zoom = 1.0;
    private int contentX = 0;
    private int contentY = 0;
    private Point currentDragOffset = null;
    private JScrollPane scrollPane;
    private Component error = null;
    private BufferedImage renderedError = null;
    private boolean dragWithLeftMouse = true;
    private Cursor standardCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

    public ImageViewerPanelCanvas(ImageViewerPanel imageViewerPanel) {
        this.imageViewerPanel = imageViewerPanel;
        setLayout(null);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
//        System.out.println(image);
        this.image = image;
        revalidate();
        repaint();
    }

    public void centerImage() {
        int availableWidth = 2;
        int availableHeight = 2;
        if (scrollPane != null) {
            availableWidth = scrollPane.getWidth();
            availableHeight = scrollPane.getHeight();
        }
        if (image.getWidth() < availableWidth) {
            contentX = availableWidth / 2 - image.getWidth() / 2;
        } else {
            contentX = 0;
        }
        if (image.getHeight() < availableHeight) {
            contentY = availableHeight / 2 - image.getHeight() / 2;
        } else {
            contentY = 0;
        }
        revalidate();
        repaint();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Dimension getPreferredSize() {
        int width = 0;
        int height = 0;
        if (image != null) {
            width = (int) (image.getWidth() * zoom) + contentX;
            height = (int) (image.getHeight() * zoom) + contentY;
        }
        if (scrollPane != null) {
            width = Math.max(scrollPane.getViewport().getWidth(), width);
            height = Math.max(scrollPane.getViewport().getHeight(), height);
        }
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D) g;
        int x = contentX;
        int y = contentY;
        int w;
        int h;

        if (image != null) {
            w = (int) (zoom * image.getWidth());
            h = (int) (zoom * image.getHeight());
        } else if (scrollPane != null) {
            w = scrollPane.getViewport().getWidth();
            h = scrollPane.getViewport().getHeight();
        } else {
            w = getWidth();
            h = getHeight();
        }

        if (image != null && error == null) {
            AffineTransform transform = new AffineTransform();
            transform.scale(zoom, zoom);
            BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            graphics2D.drawImage(image, op, contentX, contentY);
            for (ImageViewerPanelPlugin plugin : imageViewerPanel.getPlugins()) {
                plugin.postprocessDraw(graphics2D, x, y, w, h);
            }
        }
        if (error != null) {
            graphics2D.setColor(UIManager.getColor("Button.background"));
            graphics2D.fillRect(x, y, w, h);
            BufferedImage renderedError = getRenderedError();
            if (renderedError != null) {
                graphics2D.drawImage(renderedError, contentX, contentY, null);
            }
            graphics2D.setColor(UIManager.getColor("Button.borderColor"));
            graphics2D.drawRect(x, y, w, h);
        }
    }

    public double getZoom() {
        return zoom;
    }

    public void setZoom(double zoom) {
        zoom = Math.max(zoom, 10e-4);
        Point mousePosition = getMousePosition();
        Point2D.Double currentPixel = null;
        if (mousePosition != null) {
            currentPixel = screenToImageSubPixelCoordinate(mousePosition, false);
        }
        this.zoom = zoom;
        if (currentPixel != null) {
            Point2D.Double newPixelLocation = imageSubPixelCoordinateToScreen(currentPixel);
            double dx = newPixelLocation.x - mousePosition.x;
            double dy = newPixelLocation.y - mousePosition.y;
            contentX -= (int) dx;
            contentY -= (int) dy;
        }
        revalidate();
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        eventBus.post(new MouseClickedEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isMiddleMouseButton(e) || (dragWithLeftMouse && SwingUtilities.isLeftMouseButton(e))) {
            currentDragOffset = new Point(e.getPoint().x - contentX, e.getPoint().y - contentY);
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        eventBus.post(new MousePressedEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        currentDragOffset = null;
        setCursor(standardCursor);
        eventBus.post(new MouseReleasedEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        eventBus.post(new MouseEnteredEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        eventBus.post(new MouseExitedEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (currentDragOffset != null) {
            Point newLocation = e.getPoint();
            int withOffsetX = newLocation.x - currentDragOffset.x;
            int withOffsetY = newLocation.y - currentDragOffset.y;
            this.contentX = withOffsetX;
            this.contentY = withOffsetY;
            fixNegativeOffsets();
            revalidate();
            repaint();
        } else {
            eventBus.post(new PixelHoverEvent(getMouseModelPixelCoordinate(false), e));
        }
        eventBus.post(new MouseDraggedEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    private void fixNegativeOffsets() {
        int w;
        int h;
        if (image != null) {
            w = (int) (zoom * image.getWidth());
            h = (int) (zoom * image.getHeight());
        } else if (scrollPane != null) {
            w = scrollPane.getViewport().getWidth();
            h = scrollPane.getViewport().getHeight();
        } else {
            w = getWidth();
            h = getHeight();
        }
        contentX = Math.max(-w + 10, contentX);
        contentY = Math.max(-h + 10, contentY);
//        if (contentX < 0) {
//            int d = -contentX;
//            contentX = 0;
//            if (scrollPane != null) {
//                scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getValue() + d);
//            }
//        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        eventBus.post(new PixelHoverEvent(getMouseModelPixelCoordinate(false), e));
        eventBus.post(new MouseMovedEvent(e.getComponent(),
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX(),
                e.getY(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton()));
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public void setScrollPane(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }

    public Component getError() {
        return error;
    }

    public void setError(Component error) {
        this.error = error;
        this.renderedError = null;
        revalidate();
        repaint();
    }

    private BufferedImage createImage(Component panel) {
        Dimension size = new Dimension(panel.getBounds().width, panel.getBounds().height);
        BufferedImage image = new BufferedImage(
                size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        panel.paint(g2d);
        g2d.dispose();
        return image;
    }

    private BufferedImage getRenderedError() {
        if (error != null) {
            int w;
            int h;

            if (image != null) {
                w = (int) (zoom * image.getWidth());
                h = (int) (zoom * image.getHeight());
            } else if (scrollPane != null) {
                w = scrollPane.getViewport().getWidth();
                h = scrollPane.getViewport().getHeight();
            } else {
                w = getWidth();
                h = getHeight();
            }
            if (renderedError != null) {
                if (renderedError.getWidth() == w && renderedError.getHeight() == h)
                    return renderedError;
            }
            error.setBounds(0, 0, w, h);
            renderedError = createImage(error);
        }
        return renderedError;
    }

    /**
     * Finds the screen coordinate for a pixel coordinate
     *
     * @param subPixelCoordinate the pixel coordinate
     * @return the screen coordinate
     */
    public Point2D.Double imageSubPixelCoordinateToScreen(Point2D.Double subPixelCoordinate) {
        double x = subPixelCoordinate.x;
        double y = subPixelCoordinate.y;
        double zx = x * zoom;
        double zy = y * zoom;
        return new Point2D.Double(zx + contentX, zy + contentY);
    }

    /**
     * Converts screen coordinates into image pixel coordinates
     *
     * @param screenCoordinate the screen coordinates
     * @param checkBounds      If true, check for bounds (if false, negative and larger than the image coordinates will be returned)
     * @return the pixel coordinates. Null if the checkBounds is true and the coordinate is outside the image coordinates
     */
    public Point2D.Double screenToImageSubPixelCoordinate(Point screenCoordinate, boolean checkBounds) {
        int x = screenCoordinate.x;
        int y = screenCoordinate.y;
        x -= contentX;
        y -= contentY;
        if (checkBounds && (x < 0 || y < 0))
            return null;
        int sw = (int) (zoom * image.getWidth());
        int sh = (int) (zoom * image.getHeight());
        if (checkBounds && (x >= sw || y >= sh))
            return null;
        double rx;
        double ry;
        if (checkBounds) {
            rx = Math.max(0, Math.min(1, 1.0 * x / sw));
            ry = Math.max(0, Math.min(1, 1.0 * y / sh));
        } else {
            rx = 1.0 * x / sw;
            ry = 1.0 * y / sh;
        }
        double mx = (rx * image.getWidth());
        double my = (ry * image.getHeight());
        return new Point2D.Double(mx, my);
    }


    /**
     * Converts screen coordinates into image pixel coordinates
     *
     * @param screenCoordinate the screen coordinates
     * @param checkBounds      If true, check for bounds (if false, negative and larger than the image coordinates will be returned)
     * @return the pixel coordinates. Null if the checkBounds is true and the coordinate is outside the image coordinates
     */
    public Point screenToImageCoordinate(Point screenCoordinate, boolean checkBounds) {
        int x = screenCoordinate.x;
        int y = screenCoordinate.y;
        x -= contentX;
        y -= contentY;
        if (checkBounds && (x < 0 || y < 0))
            return null;
        int sw = (int) (zoom * image.getWidth());
        int sh = (int) (zoom * image.getHeight());
        if (checkBounds && (x >= sw || y >= sh))
            return null;
        double rx;
        double ry;
        if (checkBounds) {
            rx = Math.max(0, Math.min(1, 1.0 * x / sw));
            ry = Math.max(0, Math.min(1, 1.0 * y / sh));
        } else {
            rx = 1.0 * x / sw;
            ry = 1.0 * y / sh;
        }
        int mx = (int) (rx * image.getWidth());
        int my = (int) (ry * image.getHeight());
        return new Point(mx, my);
    }

    /**
     * Gets the pixel coordinates inside the shown image under the mouse.
     *
     * @param checkBounds If true, check for bounds (if false, negative and larger than the image coordinates will be returned)
     * @return the pixel coordinates. Null if the current mouse position is invalid.
     */
    public Point getMouseModelPixelCoordinate(boolean checkBounds) {
        if (image == null)
            return null;
        Point mousePosition = getMousePosition();
        if (mousePosition != null) {
            return screenToImageCoordinate(mousePosition, checkBounds);
        }
        return null;
    }

    public int getContentX() {
        return contentX;
    }

    public void setContentX(int contentX) {
        this.contentX = contentX;
        revalidate();
        repaint();
    }

    public int getContentY() {
        return contentY;
    }

    public void setContentY(int contentY) {
        this.contentY = contentY;
        revalidate();
        repaint();
    }

    public void setContentXY(int x, int y) {
        this.contentX = x;
        this.contentY = y;
        revalidate();
        repaint();
    }

    public boolean isDragWithLeftMouse() {
        return dragWithLeftMouse;
    }

    public void setDragWithLeftMouse(boolean dragWithLeftMouse) {
        this.dragWithLeftMouse = dragWithLeftMouse;
    }

    public Cursor getStandardCursor() {
        return standardCursor;
    }

    public void setStandardCursor(Cursor standardCursor) {
        this.standardCursor = standardCursor;
        setCursor(standardCursor);
    }

    public static class PixelHoverEvent {
        private final Point pixelCoordinate;
        private final MouseEvent mouseEvent;

        public PixelHoverEvent(Point pixelCoordinate, MouseEvent mouseEvent) {
            this.pixelCoordinate = pixelCoordinate;
            this.mouseEvent = mouseEvent;
        }

        public Point getPixelCoordinate() {
            return pixelCoordinate;
        }

        public MouseEvent getMouseEvent() {
            return mouseEvent;
        }
    }
}
