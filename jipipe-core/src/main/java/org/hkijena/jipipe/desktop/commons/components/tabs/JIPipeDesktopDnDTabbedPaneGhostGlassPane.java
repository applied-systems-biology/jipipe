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

package org.hkijena.jipipe.desktop.commons.components.tabs;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class JIPipeDesktopDnDTabbedPaneGhostGlassPane extends JComponent {
    private static final AlphaComposite ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f);
    public final JIPipeDesktopDnDTabbedPane tabbedPane;
    private final Rectangle lineRect = new Rectangle();
    private final Color lineColor = new Color(0, 100, 255);
    private final Point location = new Point();
    private transient BufferedImage draggingGhost;

    protected JIPipeDesktopDnDTabbedPaneGhostGlassPane(JIPipeDesktopDnDTabbedPane tabbedPane) {
        super();
        this.tabbedPane = tabbedPane;
        setOpaque(false);
        // [JDK-6700748] Cursor flickering during D&D when using CellRendererPane with validation - Java Bug System
        // https://bugs.openjdk.java.net/browse/JDK-6700748
        // setCursor(null);
    }

    public void setTargetRect(int x, int y, int width, int height) {
        lineRect.setBounds(x, y, width, height);
    }

    public void setImage(BufferedImage draggingImage) {
        this.draggingGhost = draggingImage;
    }

    public void setPoint(Point pt) {
        this.location.setLocation(pt);
    }

    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        if (!v) {
            setTargetRect(0, 0, 0, 0);
            setImage(null);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(ALPHA);
        if (tabbedPane.isPaintScrollArea && tabbedPane.getTabLayoutPolicy() == JTabbedPane.SCROLL_TAB_LAYOUT) {
            g2.setPaint(Color.RED);
            g2.fill(tabbedPane.rectBackward);
            g2.fill(tabbedPane.rectForward);
        }
        if (draggingGhost != null) {
            double xx = location.getX() - draggingGhost.getWidth(this) / 2d;
            double yy = location.getY() - draggingGhost.getHeight(this) / 2d;
            g2.drawImage(draggingGhost, (int) xx, (int) yy, this);
        }
        g2.setPaint(lineColor);
        g2.fill(lineRect);
        g2.dispose();
    }
}
