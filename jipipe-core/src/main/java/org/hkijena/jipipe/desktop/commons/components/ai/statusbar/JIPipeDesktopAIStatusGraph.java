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

package org.hkijena.jipipe.desktop.commons.components.ai.statusbar;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.ai.llm.activity.LLMActivityEntry;
import org.hkijena.jipipe.api.ai.llm.activity.LLMActivityPhase;
import org.hkijena.jipipe.api.ai.llm.activity.LLMActivityTracker;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.app.automation.JIPipeDesktopMCPIntegration;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEvent;
import org.hkijena.jipipe.desktop.app.automation.MCPStatusEventListener;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * A two-row, wall-clock activity timeline for the AI features.
 *
 * <p>This component reads the rolling phase history from
 * {@link JIPipeAIServiceComponent#getLLMActivityTracker()} and renders it as a
 * timeline bar chart (left = old, right = now):</p>
 * <ul>
 *     <li><b>Top row (LLM/agent activity)</b>: bars for {@link LLMActivityPhase#Thinking}
 *         (reasoning), {@link LLMActivityPhase#Response} (content) and
 *         {@link LLMActivityPhase#ToolCall} (agent tool calls, which in this codebase are
 *         the MCP operation dispatches), each colored via
 *         {@link ThemeUtils#getNodeFillColor(float)} from the phase's hue.</li>
 *     <li><b>Bottom row (embedding server)</b>: bars for
 *         {@link LLMActivityPhase#Embedding}.</li>
 * </ul>
 * <p>
 * The visible window is the last {@link #WINDOW_MS} milliseconds; entries older
 * than the window are clipped off the left. The in-progress phase (if any) is
 * drawn from its start time up to "now" with a theme border outline.
 * </p>
 * <p>
 * Left-aligned text (mirroring {@link org.hkijena.jipipe.desktop.commons.components.tools.JIPipeDesktopMemoryGraph})
 * shows two measured lines: the current AI phase (with tokens/s when available) and
 * the MCP indicator ("MCP off", "MCP starting...", "MCP paused", "MCP ready", or
 * "MCP: &lt;operation id&gt;"). Both lines and the compact variant's preferred height are derived
 * from the font metrics (never hardcoded pixel sizes); long operation ids are
 * ellipsized to the available width.
 * </p>
 * <p>
 * Sizing has two variants: the compact status bar variant derives its preferred (and
 * minimum) height from the font metrics; when an explicit preferred size is set (the
 * large chart variant, e.g. the AI monitor's overview page), that chart design size is
 * honored and the minimum height stays at a legible chart height
 * ({@link #LARGE_MINIMUM_HEIGHT}) so the two timeline rows and the legend cannot
 * collapse to a text-only sliver.
 * </p>
 * <p>
 * The MCP line is refreshed by a weak subscription to
 * {@link JIPipeDesktopMCPIntegration#STATUS_EVENTS} (the emitter is static and outlives
 * the component, so a disposed graph is collected together with its subscription) and
 * by the same 1000 ms repaint cadence as the memory graph.
 * </p>
 */
public class JIPipeDesktopAIStatusGraph extends JPanel implements MCPStatusEventListener {

    /**
     * Retained for API parallelism with the memory graph (unused by the timeline).
     */
    private static final int RESOLUTION = 100;

    /**
     * Horizontal/vertical padding around the drawn area.
     */
    private static final int PADDING = 2;

    /**
     * Text x-origin, matching the memory graph's {@code paintText}.
     */
    private static final int TEXT_X = 4;

    /**
     * Vertical gap between the two text lines.
     */
    private static final int TEXT_LINE_GAP = 1;

    /**
     * Default preferred width. The manager embeds the graph with this width.
     */
    private static final int PREFERRED_WIDTH = 200;

    /**
     * Minimum chart height of the large variant (an explicitly set preferred size selects
     * it): keeps the two timeline rows and the legend legible when the layout squeezes
     * the component below its preferred height. A pure chart widget design size (the
     * same pattern as the memory graph); the overlay text stays measured and ellipsized.
     */
    private static final int LARGE_MINIMUM_HEIGHT = 120;

    /**
     * Width of the visible wall-clock window in milliseconds. The right edge is
     * "now"; entries older than {@code now - WINDOW_MS} are clipped. 60 s is a
     * sensible default for a status-bar glance.
     */
    private static final long WINDOW_MS = 60_000L;

    private final Color lineColor;
    private final Color idleColor;
    private final Font infoFont;
    private final Timer repaintTimer;

    /**
     * Retained for API compatibility / parallelism with the memory graph; no-op.
     */
    @SuppressWarnings("unused")
    private final boolean withMaxCache;

    private boolean showLegend = false;

    /**
     * Creates a new AI activity timeline graph.
     *
     * @param fontSize     the font size for the status info text
     * @param withMaxCache retained for API compatibility with the memory graph (no-op)
     */
    public JIPipeDesktopAIStatusGraph(int fontSize, boolean withMaxCache) {
        this.withMaxCache = withMaxCache;
        this.lineColor = ThemeUtils.getCurrentStyle().getBorderColor();
        this.idleColor = ThemeUtils.getCurrentStyle().getTextMuted();
        this.infoFont = new Font(Font.DIALOG, Font.PLAIN, fontSize);

        // 1000 ms repaint cadence, mirroring the memory graph (NOT the old 250 ms).
        repaintTimer = new Timer(1000, e -> {
            if (isDisplayable()) {
                UIUtils.repaintLater(this);
            } else {
                stopRepaintTimer();
            }
        });
        repaintTimer.setRepeats(true);

        // MCP indicator: weak subscription (the static emitter outlives this component).
        JIPipeDesktopMCPIntegration.STATUS_EVENTS.subscribeWeak(this);

        initialize();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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

    public boolean isShowLegend() {
        return showLegend;
    }

    public void setShowLegend(boolean showLegend) {
        this.showLegend = showLegend;
    }

    /**
     * The preferred height for the current font: the two overlay text lines plus
     * padding, measured from the font metrics (never hardcoded).
     */
    private int computePreferredHeight() {
        FontMetrics fontMetrics = getFontMetrics(infoFont);
        return PADDING * 2 + 2 * fontMetrics.getHeight() + TEXT_LINE_GAP;
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            // Large chart variant: an explicitly set preferred size (the chart design
            // size, e.g. 400x200 on the overview page) wins over the compact
            // metrics-derived size.
            return super.getPreferredSize();
        }
        return new Dimension(PREFERRED_WIDTH, computePreferredHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        Dimension preferred = getPreferredSize();
        if (isPreferredSizeSet()) {
            // Large chart variant: never collapse below a legible chart height (the two
            // timeline rows plus the legend).
            return new Dimension(preferred.width, Math.min(preferred.height, LARGE_MINIMUM_HEIGHT));
        }
        return preferred;
    }

    @Override
    public void onMCPStatusChanged(MCPStatusEvent event) {
        // Status events arrive from background threads; marshal to the EDT.
        SwingUtilities.invokeLater(() -> UIUtils.repaintLater(this));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }

            List<LLMActivityEntry> history = null;
            LLMActivityPhase currentPhase = LLMActivityPhase.Idle;
            long phaseStartMs = 0;
            int currentTokens = 0;
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            if (aiService != null) {
                LLMActivityTracker tracker = aiService.getLLMActivityTracker();
                if (tracker != null) {
                    history = tracker.getHistory();
                    currentPhase = tracker.getCurrentPhase();
                    phaseStartMs = tracker.getPhaseStartMs();
                    currentTokens = tracker.getAccumulatedTokens();
                }
            }

            long now = System.currentTimeMillis();

            // Inner drawing area (2px padding each side), matching the memory graph.
            int innerLeft = PADDING;
            int innerRight = width - PADDING;
            int innerTop = PADDING;
            int innerBottom = height - PADDING;
            int innerHeight = innerBottom - innerTop;
            if (innerHeight <= 0) {
                return;
            }
            double graphWidth = innerRight - innerLeft;

            // Two-row split: 60% LLM/agent (top), 40% embedding (bottom), with a 1px separator.
            int separatorY = innerTop + (int) Math.round(innerHeight * 0.6);
            int llmTop = innerTop;
            int llmBottom = Math.max(llmTop, separatorY - 1);
            int embTop = Math.min(innerBottom, separatorY + 1);
            int embBottom = innerBottom;

            // --- Draw finalized history bars ---
            if (history != null) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                for (LLMActivityEntry entry : history) {
                    if (entry.endTimestampMs() < now - WINDOW_MS) {
                        continue; // clipped off the left edge
                    }
                    int x1 = mapTimeToX(entry.startTimestampMs(), now, innerLeft, graphWidth);
                    int x2 = mapTimeToX(entry.endTimestampMs(), now, innerLeft, graphWidth);
                    x1 = clamp(x1, innerLeft, innerRight);
                    x2 = clamp(x2, innerLeft, innerRight);
                    int barWidth = x2 - x1;
                    if (barWidth <= 0) {
                        continue;
                    }
                    int[] band = bandFor(entry.phase(), llmTop, llmBottom, embTop, embBottom);
                    if (band == null) {
                        continue;
                    }
                    g2d.setColor(colorFor(entry.phase()));
                    g2d.fillRect(x1, band[0], barWidth, band[1] - band[0]);
                }

                // --- Draw the in-progress phase (with a border outline) ---
                if (currentPhase != LLMActivityPhase.Idle && phaseStartMs > 0) {
                    int x1 = mapTimeToX(phaseStartMs, now, innerLeft, graphWidth);
                    int x2 = mapTimeToX(now, now, innerLeft, graphWidth);
                    x1 = clamp(x1, innerLeft, innerRight);
                    x2 = clamp(x2, innerLeft, innerRight);
                    int barWidth = x2 - x1;
                    int[] band = bandFor(currentPhase, llmTop, llmBottom, embTop, embBottom);
                    if (band != null && barWidth > 0) {
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                        g2d.setColor(colorFor(currentPhase));
                        g2d.fillRect(x1, band[0], barWidth, band[1] - band[0]);

                        // Outline the active bar at full alpha.
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        g2d.setColor(ThemeUtils.getNodeBorderColor(currentPhase.getHue()));
                        g2d.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2d.drawRect(x1, band[0], barWidth, band[1] - band[0] - 1);
                    }
                }
            }

            // --- Separator line between the two rows ---
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(1f));
            g2d.drawLine(innerLeft, separatorY, innerRight, separatorY);

            // --- Left-aligned text (AI phase line + MCP indicator line) ---
            paintText(history, currentPhase, currentTokens, g2d, width);

            if (showLegend) {
                paintLegend(g2d, width, height);
            }

        } finally {
            g2d.dispose();
        }
    }

    /**
     * Draws the two left-aligned status lines, mirroring the memory graph's
     * {@code paintText}: {@code x=TEXT_X}, baselines measured from the font metrics,
     * color {@code getTextForeground()}, font {@code Font.DIALOG}.
     *
     * <p>Line 1 is the AI phase: {@code "AI: <phase>"} plus {@code "<tokens> tok/s"}
     * when a non-zero tokens/s value is available (from the in-progress phase or the
     * most recently finalized entry); {@code "AI: Idle"} when idle. Line 2 is the MCP
     * indicator from {@link JIPipeDesktopMCPIntegration#currentSnapshot()}. Both lines
     * are ellipsized to the available width.</p>
     */
    private void paintText(List<LLMActivityEntry> history,
                           LLMActivityPhase currentPhase,
                           int currentTokens,
                           Graphics2D g2d,
                           int width) {
        g2d.setFont(infoFont);
        FontMetrics fontMetrics = g2d.getFontMetrics(infoFont);
        g2d.setColor(ThemeUtils.getCurrentStyle().getTextForeground());

        int maxWidth = width - TEXT_X - PADDING;

        int tokensPerSec = currentTokens;
        if ((tokensPerSec == 0) && history != null && !history.isEmpty()) {
            LLMActivityEntry last = history.get(history.size() - 1);
            if (last.tokenCount() > 0) {
                tokensPerSec = last.tokenCount();
            }
        }

        String aiText;
        if (currentPhase != LLMActivityPhase.Idle) {
            aiText = "AI: " + currentPhase.getShortLabel();
            if (tokensPerSec > 0) {
                aiText += " " + tokensPerSec + " tok/s";
            }
        } else {
            aiText = "AI: Idle";
        }

        int line1Baseline = PADDING + fontMetrics.getAscent();
        int line2Baseline = PADDING + fontMetrics.getHeight() + TEXT_LINE_GAP + fontMetrics.getAscent();

        g2d.drawString(ellipsize(aiText, fontMetrics, maxWidth), TEXT_X, line1Baseline);
        g2d.drawString(ellipsize(mcpStatusText(), fontMetrics, maxWidth), TEXT_X, line2Baseline);
    }

    /**
     * The MCP indicator text for the second overlay line, from the current status
     * snapshot. An operation in flight shows its id (the busy spinner semantics stay
     * out of the text).
     */
    private String mcpStatusText() {
        JIPipeDesktopMCPIntegration.StatusSnapshot snapshot = JIPipeDesktopMCPIntegration.currentSnapshot();
        switch (snapshot.state()) {
            case Off:
                return "MCP off";
            case Starting:
                return "MCP starting...";
            case Paused:
                return "MCP paused";
            case Running:
            default:
                if (snapshot.lastOperationId() != null && !snapshot.lastOperationId().isEmpty()) {
                    return "MCP: " + snapshot.lastOperationId();
                }
                return "MCP ready";
        }
    }

    /**
     * Ellipsizes a text to the given pixel width with a trailing "..." (measured, never
     * assumed). Returns an empty string when not even the ellipsis fits.
     */
    private String ellipsize(String text, FontMetrics fontMetrics, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (fontMetrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        if (fontMetrics.stringWidth(ellipsis) > maxWidth) {
            return "";
        }
        int lo = 0;
        int hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (fontMetrics.stringWidth(text.substring(0, mid) + ellipsis) <= maxWidth) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        return text.substring(0, lo) + ellipsis;
    }

    /**
     * Maps a wall-clock timestamp to an x coordinate within the visible window.
     *
     * @param t          the timestamp (ms)
     * @param now        current time (right edge)
     * @param innerLeft  left x of the inner area
     * @param graphWidth pixel width of the inner area
     * @return the x coordinate (not clamped)
     */
    private int mapTimeToX(long t, long now, int innerLeft, double graphWidth) {
        double frac = (double) (t - (now - WINDOW_MS)) / (double) WINDOW_MS;
        return innerLeft + (int) Math.round(frac * graphWidth);
    }

    /**
     * Selects the vertical band (top/bottom y) for a phase's row.
     *
     * @param phase     the phase
     * @param llmTop    top y of the LLM row
     * @param llmBottom bottom y of the LLM row
     * @param embTop    top y of the embedding row
     * @param embBottom bottom y of the embedding row
     * @return {@code {topY, bottomY}} or null if the phase maps to no row
     */
    private int[] bandFor(LLMActivityPhase phase, int llmTop, int llmBottom, int embTop, int embBottom) {
        if (phase == LLMActivityPhase.Embedding) {
            return new int[]{embTop, embBottom};
        } else if (phase == LLMActivityPhase.Thinking
                || phase == LLMActivityPhase.Response
                || phase == LLMActivityPhase.ToolCall) {
            return new int[]{llmTop, llmBottom};
        }
        return null;
    }

    /**
     * Theme-derived color for a phase. Idle uses {@code getTextMuted()} (the hue
     * API returns the panel background for the {@code -1} hue, which is not
     * useful as a busy bar color).
     */
    private Color colorFor(LLMActivityPhase phase) {
        if (phase == LLMActivityPhase.Idle || phase.getHue() < 0) {
            return idleColor;
        }
        return ThemeUtils.getNodeFillColor(phase.getHue());
    }

    private void paintLegend(Graphics2D g2d, int width, int height) {
        LLMActivityPhase[] phases = {
                LLMActivityPhase.Thinking,
                LLMActivityPhase.Response,
                LLMActivityPhase.ToolCall,
                LLMActivityPhase.Embedding,
                LLMActivityPhase.Idle
        };
        String[] labels = {"Thinking", "Response", "Tool call", "Embedding", "Idle"};

        g2d.setFont(infoFont);
        FontMetrics fm = g2d.getFontMetrics(infoFont);

        int swatchSize = 8;
        int swatchGap = 4;
        int lineGap = 2;
        int padding = 6;

        int rowHeight = Math.max(swatchSize, fm.getHeight()) + lineGap;
        int legendWidth = 0;
        for (String label : labels) {
            int w = swatchSize + swatchGap + fm.stringWidth(label);
            legendWidth = Math.max(legendWidth, w);
        }
        legendWidth += padding * 2;
        int legendHeight = rowHeight * labels.length - lineGap + padding * 2;

        int legendX = 4;
        int legendY = height - legendHeight - 4;

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        g2d.setColor(ThemeUtils.getCurrentStyle().getWindowBackground());
        g2d.fillRoundRect(legendX, legendY, legendWidth, legendHeight, 6, 6);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(ThemeUtils.getCurrentStyle().getBorderColor());
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(legendX, legendY, legendWidth, legendHeight, 6, 6);

        for (int i = 0; i < phases.length; i++) {
            int rowY = legendY + padding + i * rowHeight;
            int swatchX = legendX + padding;
            int textX = swatchX + swatchSize + swatchGap;

            g2d.setColor(colorFor(phases[i]));
            g2d.fillRect(swatchX, rowY + (fm.getHeight() - swatchSize) / 2, swatchSize, swatchSize);
            g2d.setColor(ThemeUtils.getCurrentStyle().getBorderColor());
            g2d.drawRect(swatchX, rowY + (fm.getHeight() - swatchSize) / 2, swatchSize, swatchSize);

            g2d.setColor(ThemeUtils.getCurrentStyle().getTextForeground());
            g2d.drawString(labels[i], textX, rowY + fm.getAscent());
        }
    }
}
