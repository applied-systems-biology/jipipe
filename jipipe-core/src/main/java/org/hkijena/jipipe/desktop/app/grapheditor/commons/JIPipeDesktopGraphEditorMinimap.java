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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralUIApplicationSettings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * Renders a overview of the graph
 */
public class JIPipeDesktopGraphEditorMinimap extends JIPipeDesktopWorkbenchPanel implements MouseListener, MouseMotionListener, AdjustmentListener, JIPipeDesktopGraphCanvasUI.GraphCanvasUpdatedEventListener {

    private static final Color AREA_FILL_COLOR = new Color(0x3365a4e3, true);
    private final AbstractJIPipeDesktopGraphEditorUI graphEditorUI;
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
    private Color minimapBackground;


    /**
     * @param graphEditorUI the workbench
     */
    public JIPipeDesktopGraphEditorMinimap(AbstractJIPipeDesktopGraphEditorUI graphEditorUI) {
        super(graphEditorUI.getDesktopWorkbench());
        this.graphEditorUI = graphEditorUI;
        if (JIPipeGeneralUIApplicationSettings.getInstance().getTheme().isDark())
            minimapBackground = Color.BLACK;
        else
            minimapBackground = Color.WHITE;
        setOpaque(false);
        refreshGraphImage();

        addMouseListener(this);
        addMouseMotionListener(this);
        graphEditorUI.getScrollPane().getHorizontalScrollBar().addAdjustmentListener(this);
        graphEditorUI.getScrollPane().getVerticalScrollBar().addAdjustmentListener(this);
        graphEditorUI.getCanvasUI().getGraphCanvasUpdatedEventEmitter().subscribeWeak(this);
        graphEditorUI.getCanvasUI().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (isDisplayable()) {
                    refreshGraphImage();
                    repaint();
                }
            }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) > 0 && JIPipeDesktopGraphEditorMinimap.this.isShowing()) {
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
        if (scaleFactor != 0) {
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
        refreshView();
    }

    private void refreshView() {
        int graphImageWidth;
        int graphImageHeight;
        graphImageWidth = graphEditorUI.getCanvasUI().getWidth();
        graphImageHeight = graphEditorUI.getCanvasUI().getHeight();

        if (graphImageWidth <= 0)
            graphImageWidth = getWidth();
        if (graphImageHeight <= 0)
            graphImageHeight = getHeight();

        viewBaseWidth = getWidth();
        viewBaseHeight = getHeight();
        double factorWidth = 1.0 * getWidth() / graphImageWidth;
        double factorHeight = 1.0 * getHeight() / graphImageHeight;
        scaleFactor = Math.min(factorWidth, factorHeight);
        viewWidth = (int) (graphImageWidth * scaleFactor);
        viewHeight = (int) (graphImageHeight * scaleFactor);
        viewX = getWidth() / 2 - viewWidth / 2;
        viewY = getHeight() / 2 - viewHeight / 2;
        scrollWidth = (int) (scaleFactor * graphEditorUI.getScrollPane().getHorizontalScrollBar().getVisibleAmount());
        scrollHeight = (int) (scaleFactor * graphEditorUI.getScrollPane().getVerticalScrollBar().getVisibleAmount());
        scrollX = (int) (scaleFactor * graphEditorUI.getScrollPane().getHorizontalScrollBar().getValue());
        scrollY = (int) (scaleFactor * graphEditorUI.getScrollPane().getVerticalScrollBar().getValue());
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(UIManager.getColor("Panel.background"));
        g.fillRect(0, 0, getWidth(), getHeight());
        if (viewBaseWidth != getWidth() || viewBaseHeight != getHeight()) {
            refreshView();
        }

        Graphics2D graphics2D = (Graphics2D) g;

        graphics2D.setColor(minimapBackground);
        graphics2D.fillRect(viewX, viewY, viewWidth, viewHeight);
        graphEditorUI.getCanvasUI().paintMiniMap(graphics2D, scaleFactor, viewX, viewY);

        // Draw current scroll position
        g.setColor(AREA_FILL_COLOR);
        g.fillRect(viewX + scrollX, viewY + scrollY, scrollWidth, scrollHeight);
        g.setColor(JIPipeDesktopModernMetalTheme.PRIMARY5);
        ((Graphics2D) g).setStroke(new BasicStroke(2));
        g.drawRect(viewX + scrollX, viewY + scrollY, scrollWidth, scrollHeight);

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

    @Override
    public void onGraphCanvasUpdated(JIPipeDesktopGraphCanvasUI.GraphCanvasUpdatedEvent event) {
        if (isDisplayable()) {
            refreshGraphImage();
            repaint();
        }
    }
}
