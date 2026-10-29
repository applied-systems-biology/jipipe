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

package org.hkijena.jipipe.desktop.commons.components.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.JIPipeMemoryStatus;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * A component that displays a real-time memory usage graph.
 *
 * <p>This component reads memory history from the {@link org.hkijena.jipipe.api.service.components.JIPipeMemoryMonitorServiceComponent}
 * and renders it as a visual graph. It handles its own repainting on a timer to ensure
 * the display stays updated.</p>
 */
public class JIPipeDesktopMemoryGraph extends JPanel {
    private static final int RESOLUTION = 100;
    private static final double WARNING_LIMIT = 0.9;
    private final Color usageColorNormal;
    private final Color usageColorWarning;
    private final Color cacheColor;
    private final Color scratchLimitColor;
    private final Font infoFont;
    private final Timer repaintTimer;
    // Retained for API compatibility with the source branch; cache painting was removed.
    @SuppressWarnings("unused")
    private final boolean withMaxCache;

    /**
     * Creates a new memory graph component.
     *
     * @param fontSize     the font size for the memory info text
     * @param withMaxCache retained for API compatibility with the source branch (no-op)
     */
    public JIPipeDesktopMemoryGraph(int fontSize, boolean withMaxCache) {
        this.withMaxCache = withMaxCache;
        this.usageColorNormal = ThemeUtils.getCurrentStyle().getPrimaryColor();
        this.cacheColor = ThemeUtils.getCurrentStyle().getSuccessColor();
        this.usageColorWarning = ThemeUtils.getCurrentStyle().getWarningColor();
        this.scratchLimitColor = ThemeUtils.getCurrentStyle().getDangerColor();
        this.infoFont = new Font(Font.DIALOG, Font.PLAIN, fontSize);

        // Create a simple repaint timer to update the display
        repaintTimer = new Timer(1000, e -> {
            if (isDisplayable()) {
                UIUtils.repaintLater(this);
            } else {
                stopRepaintTimer();
            }
        });
        repaintTimer.setRepeats(true);

        initialize();
    }

    private void initialize() {
        setOpaque(false);
    }

    /**
     * Starts the repaint timer to update the graph display.
     */
    public void startRepaintTimer() {
        repaintTimer.start();
    }

    /**
     * Stops the repaint timer.
     */
    public void stopRepaintTimer() {
        repaintTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Get memory history from the service
            List<JIPipeMemoryStatus> memoryStatuses = JIPipe.getMemoryMonitor().getHistory();

            if (memoryStatuses.isEmpty()) {
                return;
            }

            int width = getWidth();
            int height = getHeight();

            // Graph dimensions
            double graphWidth = width - 4; // 2px padding on each side
            double graphHeight = height - 4; // 2px padding on each side
            double xStep = graphWidth / (RESOLUTION - 1);

            // Draw usageBytes line (dark blue) with background
            paintMemoryUsage(memoryStatuses, xStep, graphHeight, graphWidth, g2d);

            // Paint text
            paintText(memoryStatuses, g2d);

        } finally {
            g2d.dispose();
        }
    }

    private void paintText(List<JIPipeMemoryStatus> memoryStatuses, Graphics2D g2d) {
        JIPipeMemoryStatus current = memoryStatuses.getLast();
        g2d.setFont(infoFont);
        FontMetrics fontMetrics = g2d.getFontMetrics(infoFont);
        g2d.setColor(ThemeUtils.getCurrentStyle().getTextForeground());

        String text = StringUtils.formatBytes(current.usedMemory()) + "/" + StringUtils.formatBytes(current.maxMemory());

        g2d.drawString(text, 4, fontMetrics.getHeight());
    }

    private void paintMemoryUsage(List<JIPipeMemoryStatus> memoryStatuses, double xStep, double graphHeight, double graphWidth, Graphics2D g2d) {
        Path2D usagePath = new Path2D.Double();
        Path2D usageBgPath = new Path2D.Double();

        usagePath.moveTo(2, 2 + graphHeight);
        usageBgPath.moveTo(2, 2 + graphHeight);

        for (int i = 0; i < memoryStatuses.size(); i++) {
            JIPipeMemoryStatus mp = memoryStatuses.get(i);
            double x = 2 + (i * xStep);
            double usageY = 2 + graphHeight - (1.0 * mp.usedMemory() / mp.maxMemory() * graphHeight);

            usagePath.lineTo(x, usageY);
            usageBgPath.lineTo(x, usageY);
        }

        JIPipeMemoryStatus lastStatus = memoryStatuses.getLast();
        boolean warning = 1.0 * lastStatus.usedMemory() / lastStatus.maxMemory() >= WARNING_LIMIT;

        usageBgPath.lineTo(2 + graphWidth, 2 + graphHeight);
        usageBgPath.closePath();

        // Draw background
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2d.setColor(warning ? usageColorWarning : usageColorNormal);
        g2d.fill(usageBgPath);

        // Draw line
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(warning ? usageColorWarning : usageColorNormal);
        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(usagePath);
    }
}
