package org.hkijena.jipipe.ui.grapheditor;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.events.GraphCanvasUpdatedEvent;
import org.hkijena.jipipe.utils.ScreenImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

/**
 * Renders a overview of the graph
 */
public class JIPipeGraphEditorMinimap extends JIPipeWorkbenchPanel implements MouseListener, MouseMotionListener, AdjustmentListener {

    private final JIPipeGraphEditorUI graphEditorUI;
    private BufferedImage graphImage;
    private double scaleFactor;
    private int viewBaseWidth;
    private int viewBaseHeight;
    private int viewX;
    private int viewY;
    private int viewWidth;
    private int viewHeight;
    private int scrollWidth;
    private int scrollHeight;
    private int scrollX;
    private int scrollY;


    /**
     * @param graphEditorUI the workbench
     */
    public JIPipeGraphEditorMinimap(JIPipeGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getWorkbench());
        this.graphEditorUI = graphEditorUI;
        setOpaque(false);
        refreshGraphImage();

        addMouseListener(this);
        addMouseMotionListener(this);
        graphEditorUI.getScrollPane().getHorizontalScrollBar().addAdjustmentListener(this);
        graphEditorUI.getScrollPane().getVerticalScrollBar().addAdjustmentListener(this);
        graphEditorUI.getCanvasUI().getEventBus().register(this);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0 && JIPipeGraphEditorMinimap.this.isShowing()) {
                SwingUtilities.invokeLater(() -> {
                    refreshGraphImage();
                    repaint();
                });
            }
        });
    }

    /**
     * Updates the scroll pane, so x and y are at the center of the view
     *
     * @param x x
     * @param y y
     */
    private void scrollTo(int x, int y) {
        if (scaleFactor != 0 && graphImage != null) {
            x -= viewX;
            y -= viewY;

            // x and y are at the center -> put them to the corner
            x -= scrollWidth / 2;
            y -= scrollHeight / 2;

            // ensure that x and y are within the view
            x = Math.max(0, Math.min(viewWidth, x));
            y = Math.max(0, Math.min(viewHeight, y));

            // Back to absolute
            x = (int) (x / scaleFactor);
            y = (int) (y / scaleFactor);

            graphEditorUI.getScrollPane().getHorizontalScrollBar().setValue(x);
            graphEditorUI.getScrollPane().getVerticalScrollBar().setValue(y);
        }
    }

    private void refreshGraphImage() {
        if (graphEditorUI.getCanvasUI().getWidth() > 0 && graphEditorUI.getCanvasUI().getHeight() > 0) {
            try {
                graphImage = ScreenImage.createImage(graphEditorUI.getCanvasUI());
            } catch (Exception e) {
                e.printStackTrace();
                graphImage = null;
            }

        }
        refreshView();
    }

    private void refreshView() {
        if (graphImage != null) {
            viewBaseWidth = getWidth();
            viewBaseHeight = getHeight();
            double factorWidth = 1.0 * getWidth() / graphImage.getWidth();
            double factorHeight = 1.0 * getHeight() / graphImage.getHeight();
            scaleFactor = Math.min(factorWidth, factorHeight);
            viewWidth = (int) (graphImage.getWidth() * scaleFactor);
            viewHeight = (int) (graphImage.getHeight() * scaleFactor);
            viewX = getWidth() / 2 - viewWidth / 2;
            viewY = getHeight() / 2 - viewHeight / 2;
            scrollWidth = (int) (scaleFactor * graphEditorUI.getScrollPane().getHorizontalScrollBar().getVisibleAmount());
            scrollHeight = (int) (scaleFactor * graphEditorUI.getScrollPane().getVerticalScrollBar().getVisibleAmount());
            scrollX = (int) (scaleFactor * graphEditorUI.getScrollPane().getHorizontalScrollBar().getValue());
            scrollY = (int) (scaleFactor * graphEditorUI.getScrollPane().getVerticalScrollBar().getValue());
        }
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (graphImage != null) {
            if (viewBaseWidth != getWidth() || viewBaseHeight != getHeight()) {
                refreshView();
            }

            Graphics2D graphics2D = (Graphics2D) g;
            AffineTransform transform = new AffineTransform();
            transform.scale(scaleFactor, scaleFactor);
//            transform.translate(viewX / scaleFactor, viewY / scaleFactor);
//            AffineTransformOp scaleOp =
//                    new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            BufferedImageOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            graphics2D.drawImage(graphImage, op, viewX, viewY);

            // Draw current scroll position
            g.setColor(Color.DARK_GRAY);
            ((Graphics2D) g).setStroke(new BasicStroke(2));
            g.drawRect(viewX + scrollX, viewY + scrollY, scrollWidth, scrollHeight);
        }
        super.paintComponent(g);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        scrollTo(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        refreshView();
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        scrollTo(e.getX(), e.getY());
    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Subscribe
    public void onGraphCanvasUpdated(GraphCanvasUpdatedEvent event) {
        if (isDisplayable()) {
            refreshGraphImage();
            repaint();
        }
    }
}
