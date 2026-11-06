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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders the flat rows of the {@link JIPipeDesktopProgressTreePanel} — the Swing implementation of
 * the v2 design contract ({@code docs/superpowers/specs/2026-11-04-progress-system-overhaul/ui/v2-progress-panel.html}).
 * <p>
 * Rows are plain data snapshots (built by the panel's refresh cycle); the renderer is stateless
 * except for its prebuilt colors. Rows are painted fresh per render call — the list renders only
 * the visible rows (virtualized), so allocations are bounded by the viewport.
 * <p>
 * Node rows: indent, expander caret, state icon (running spinner / done / failed / cancelled),
 * dim partition chip ({@code P0}), label with a faint {@code [uuid]} suffix when the label carries
 * one, {@code +N} hidden-descendants badge (collapsed only), faint {@code <n> debug} chip, inline
 * progress bar with mono {@code v/mx (pct%)} text, and a phase tag (duration / {@code running Ns} /
 * {@code failed}). Detail rows: timestamp + level + message (WARN/ERROR tinted), error rows with a
 * one-line summary and a {@code Show stack trace} toggle, dark monospace stack lines,
 * {@code Show N older records from run data…} and {@code N DEBUG records hidden — switch to Level:
 * DEBUG} action rows.
 */
public class JIPipeDesktopProgressTreeRowRenderer implements ListCellRenderer<JIPipeDesktopProgressTreeRowRenderer.Row> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int INDENT_PER_LEVEL = 22;

    /**
     * One row of the flat tree list. Sealed: the row types below.
     */
    public sealed interface Row permits NodeRow, DetailRow, StackRow, MoreRow, LoadingRow, PlaceholderRow, SkipRow {
    }

    /**
     * A node or phase row. All display data is a snapshot taken when the panel rebuilt the rows.
     */
    public static final class NodeRow implements Row {
        private final int nodeId;
        private final int depth;
        private final String partitionChip;
        private final String label;
        private final String uuidSuffix;
        private final JIPipeProgressNodeState state;
        private final boolean expandable;
        private final boolean expanded;
        private final int badgeCount;
        private final int debugCount;
        private final int progressValue;
        private final int progressMax;
        private final String phaseText;

        public NodeRow(int nodeId, int depth, String partitionChip, String label, String uuidSuffix,
                       JIPipeProgressNodeState state, boolean expandable, boolean expanded,
                       int badgeCount, int debugCount, int progressValue, int progressMax, String phaseText) {
            this.nodeId = nodeId;
            this.depth = depth;
            this.partitionChip = partitionChip;
            this.label = label;
            this.uuidSuffix = uuidSuffix;
            this.state = state;
            this.expandable = expandable;
            this.expanded = expanded;
            this.badgeCount = badgeCount;
            this.debugCount = debugCount;
            this.progressValue = progressValue;
            this.progressMax = progressMax;
            this.phaseText = phaseText;
        }

        public int getNodeId() {
            return nodeId;
        }

        public String getLabel() {
            return label;
        }

        public String getPartitionChip() {
            return partitionChip;
        }

        public JIPipeProgressNodeState getState() {
            return state;
        }

        public boolean isExpandable() {
            return expandable;
        }

        public boolean isExpanded() {
            return expanded;
        }
    }

    /**
     * One record of an expanded node (log message or error summary).
     * <p>
     * {@code line} is the record's line number — the identity used by the virtual detail layer
     * (R30) to cache and to toggle stack traces; the paged mode uses it only for identity.
     */
    public record DetailRow(int nodeId, JIPipeProgressRecord record, boolean hasStack, boolean stackOpen, int line) implements Row {
    }

    /**
     * One line of an opened stack trace (dark monospace block).
     */
    public record StackRow(int nodeId, String line) implements Row {
    }

    /**
     * "Show N older records from run data…" action row.
     */
    public record MoreRow(int nodeId, int olderCount) implements Row {
    }

    /**
     * Placeholder while a node's record page is being read.
     */
    public record LoadingRow(int nodeId) implements Row {
    }

    /**
     * A virtual detail row whose chunk is not loaded (R30). It occupies exactly one row so the
     * list extent equals the node's detail size (the proportional scrollbar); the chunk load
     * requested for the surrounding viewport replaces it with the real record rows.
     */
    public record PlaceholderRow(int nodeId) implements Row {
    }

    /**
     * A virtual detail row for a record that renders nothing in the detail area (a progress
     * {@code p}, node {@code n}, or node-finished {@code N} record, or a record of a descendant
     * node). It keeps the row↔line mapping exact and measures the real render cost of a node's
     * record flood.
     */
    public record SkipRow(int nodeId) implements Row {
    }

    private final SpinnerIcon spinnerIcon;

    // Theme snapshot (read once; a theme switch rebuilds the whole UI anyway)
    private final Color colorAccent;
    private final Color colorOk;
    private final Color colorFail;
    private final Color colorWarn;
    private final Color colorText;
    private final Color colorDim;
    private final Color colorFaint;
    private final Color colorLine;
    private final Color colorRowBackground;
    private final Color colorDetailBackground;
    private final Color colorChipBackground;
    private final Color colorStackBackground = new Color(0x2b2f36);
    private final Color colorStackForeground = new Color(0xd9dde4);

    /**
     * Font-identity cache for the fixed text-bearing columns: the widths are derived from the live
     * fonts (theme mono size for the v/mx text, the list font for the phase/status column) and stay
     * stable for as long as the font object is the same — which is what aligns the columns across
     * rows. A different font object (user font/size change → the theme reinstalls component fonts)
     * re-derives them once, not per row.
     */
    private Font measuredFont;
    private Font progressTextFont;
    private Font phaseFont;
    private int progressTextColumnWidth;
    private int phaseColumnWidth;

    /**
     * Re-derives the font-dependent geometry when the list's font changed (identity check — one
     * {@code stringWidth} per font change, not per row). Called from
     * {@link #getListCellRendererComponent} before any row is rendered.
     *
     * @param list the rendering list (its font is the basis; its font metrics do the measuring)
     */
    private void updateFontDependentWidths(JList<?> list) {
        Font listFont = list.getFont();
        if (listFont == measuredFont) {
            return;
        }
        measuredFont = listFont;
        // Monospace text in rows uses the theme's normal font size (codebase precedent:
        // JIPipeDesktopLogViewLogUI), not a hardcoded size
        progressTextFont = new Font(Font.MONOSPACED, Font.PLAIN, ThemeUtils.getCurrentStyle().getFontSizeNormal());
        phaseFont = listFont.deriveFont(listFont.getSize2D() - 0.5f);
        progressTextColumnWidth = list.getFontMetrics(progressTextFont).stringWidth("8888/8888 (888%)") + 4;
        // Widest sensible phase/status text: a run running for a full day in seconds
        phaseColumnWidth = list.getFontMetrics(phaseFont).stringWidth("running 86400s") + 6;
    }

    /**
     * @param spinnerIcon the shared running spinner (its repaint parent is the list)
     */
    public JIPipeDesktopProgressTreeRowRenderer(SpinnerIcon spinnerIcon) {
        this.spinnerIcon = spinnerIcon;
        var themeStyle = ThemeUtils.getCurrentStyle();
        colorAccent = themeStyle.getPrimaryColor();
        colorOk = themeStyle.getSuccessColor();
        colorFail = themeStyle.getDangerColor();
        colorWarn = themeStyle.getWarningColor();
        colorText = themeStyle.getTextForeground();
        colorDim = themeStyle.getTextMuted();
        colorFaint = mix(colorDim, themeStyle.getPanelBackground(), 0.45f);
        colorLine = mix(themeStyle.getBorderColor(), themeStyle.getPanelBackground(), 0.55f);
        colorRowBackground = themeStyle.getPanelBackground();
        colorDetailBackground = mix(themeStyle.getPanelBackground(), themeStyle.getTextForeground(), 0.035f);
        colorChipBackground = mix(themeStyle.getPanelBackground(), themeStyle.getTextForeground(), 0.07f);
    }

    /**
     * Mixes two colors (0.0 = a, 1.0 = b).
     */
    private static Color mix(Color a, Color b, float t) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }

    /**
     * Formats a count with locale-dependent digit grouping (the mockup's {@code 1,203} shape).
     */
    private static String formatCount(int count) {
        return NumberFormat.getIntegerInstance().format(count);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Row> list, Row value, int index, boolean isSelected, boolean cellHasFocus) {
        // Font-relative geometry: re-derive the fixed text-column widths when the font changed
        updateFontDependentWidths(list);
        if (value instanceof NodeRow nodeRow) {
            return renderNodeRow(list, nodeRow);
        } else if (value instanceof DetailRow detailRow) {
            return renderDetailRow(list, detailRow);
        } else if (value instanceof StackRow stackRow) {
            return renderStackRow(stackRow);
        } else if (value instanceof MoreRow moreRow) {
            return renderActionRow("Show " + formatCount(moreRow.olderCount()) + " older records from run data…", colorAccent);
        } else if (value instanceof LoadingRow) {
            return renderActionRow("Loading records…", colorFaint);
        } else if (value instanceof PlaceholderRow) {
            // Unloaded chunk: a lightweight placeholder row — the chunk load for the surrounding
            // viewport replaces it with the real rows
            return renderVirtualRow("…", colorFaint);
        } else if (value instanceof SkipRow) {
            // A record that renders nothing: keep the row (the extent must match the real
            // content) but mark it as empty
            return renderVirtualRow("·", mix(colorFaint, colorRowBackground, 0.45f));
        }
        return new JLabel();
    }

    private Component renderNodeRow(JList<? extends Row> list, NodeRow row) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(true);
        panel.setBackground(colorRowBackground);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorLine),
                BorderFactory.createEmptyBorder(0, 8 + row.depth * INDENT_PER_LEVEL, 0, 10)));

        // West: caret + state icon + partition chip
        JPanel west = new JPanel();
        west.setOpaque(false);
        west.setLayout(new BoxLayout(west, BoxLayout.X_AXIS));

        JLabel caretLabel = new JLabel(row.expandable ? (row.expanded ? "▼" : "▶") : "");
        caretLabel.setFont(caretLabel.getFont().deriveFont(9.0f));
        caretLabel.setForeground(colorDim);
        caretLabel.setPreferredSize(new Dimension(14, 14));
        west.add(caretLabel);

        JLabel stateLabel = new JLabel();
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        stateLabel.setPreferredSize(new Dimension(16, 16));
        if (row.state == JIPipeProgressNodeState.RUNNING) {
            stateLabel.setIcon(spinnerIcon);
        } else {
            stateLabel.setText(row.state == JIPipeProgressNodeState.OK ? "✔" : "✖");
            stateLabel.setFont(stateLabel.getFont().deriveFont(11.0f));
            stateLabel.setForeground(switch (row.state) {
                case OK -> colorOk;
                case FAILED -> colorFail;
                case CANCELLED -> colorDim;
                default -> colorDim;
            });
        }
        west.add(stateLabel);

        if (row.partitionChip != null) {
            JLabel chipLabel = new JLabel(row.partitionChip);
            chipLabel.setFont(chipLabel.getFont().deriveFont(10.0f));
            chipLabel.setForeground(colorFaint);
            chipLabel.setBackground(colorChipBackground);
            chipLabel.setOpaque(true);
            chipLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
            // Align the small chip with the text baseline
            chipLabel.setAlignmentY(0.5f);
            west.add(Box.createHorizontalStrut(2));
            west.add(chipLabel);
        }
        panel.add(west, BorderLayout.WEST);

        // Center: label (+ faint uuid suffix)
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        JLabel nameLabel = new JLabel(row.label);
        nameLabel.setForeground(colorText);
        center.add(nameLabel);
        if (row.uuidSuffix != null) {
            JLabel uuidLabel = new JLabel(row.uuidSuffix);
            uuidLabel.setFont(uuidLabel.getFont().deriveFont(uuidLabel.getFont().getSize2D() - 1.5f));
            uuidLabel.setForeground(colorFaint);
            center.add(Box.createHorizontalStrut(4));
            center.add(uuidLabel);
        }
        panel.add(center, BorderLayout.CENTER);

        // East: right-aligned fixed-order segments — [+N badge] [progress bar + v/mx text]
        // [phase/status text] — with stable widths, so bars and the status column align across
        // rows even when some rows lack progress (user report "Align")
        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));

        if (row.debugCount > 0) {
            JLabel debugChip = new JLabel(formatCount(row.debugCount) + " debug");
            debugChip.setFont(debugChip.getFont().deriveFont(11.0f));
            debugChip.setForeground(colorFaint);
            debugChip.setBackground(colorChipBackground);
            debugChip.setOpaque(true);
            debugChip.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            debugChip.setAlignmentY(0.5f);
            east.add(debugChip);
            east.add(Box.createHorizontalStrut(6));
        }

        if (!row.expanded && row.badgeCount > 0) {
            JLabel badgeLabel = new JLabel("+" + formatCount(row.badgeCount));
            badgeLabel.setFont(badgeLabel.getFont().deriveFont(11.0f));
            badgeLabel.setForeground(colorDim);
            badgeLabel.setBackground(colorChipBackground);
            badgeLabel.setOpaque(true);
            badgeLabel.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            badgeLabel.setAlignmentY(0.5f);
            east.add(badgeLabel);
            east.add(Box.createHorizontalStrut(6));
        }

        // The progress segment always occupies its fixed width (a row without determinate
        // progress leaves it empty — no bar is painted, but the phase/status column does not
        // shift); the v/mx text is right-aligned within its fixed sub-width
        JPanel progressSegment = new JPanel();
        progressSegment.setOpaque(false);
        progressSegment.setLayout(new BoxLayout(progressSegment, BoxLayout.X_AXIS));
        ProgressBar bar = new ProgressBar();
        bar.setValue(row.progressValue, row.progressMax, barColor(row));
        bar.setAlignmentY(0.5f);
        progressSegment.add(bar);
        progressSegment.add(Box.createHorizontalStrut(6));
        JLabel progressTextLabel = new JLabel(row.progressMax > 0
                ? row.progressValue + "/" + row.progressMax + " (" + progressPercent(row) + "%)"
                : "");
        progressTextLabel.setFont(progressTextFont);
        progressTextLabel.setForeground(colorDim);
        progressTextLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        progressTextLabel.setPreferredSize(new Dimension(progressTextColumnWidth, 14));
        progressTextLabel.setMaximumSize(new Dimension(progressTextColumnWidth, 14));
        progressSegment.add(progressTextLabel);
        Dimension progressSegmentSize = new Dimension(110 + 6 + progressTextColumnWidth, 18);
        progressSegment.setPreferredSize(progressSegmentSize);
        progressSegment.setMaximumSize(progressSegmentSize);
        progressSegment.setAlignmentY(0.5f);
        east.add(progressSegment);

        // The phase/status text is the last column: fixed (font-derived) width, right-aligned
        // (rows without a phase reserve the same space)
        JLabel phaseLabel = new JLabel(row.phaseText == null ? "" : row.phaseText);
        phaseLabel.setFont(phaseFont);
        phaseLabel.setForeground(colorFaint);
        phaseLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        phaseLabel.setPreferredSize(new Dimension(phaseColumnWidth, 14));
        phaseLabel.setMaximumSize(new Dimension(phaseColumnWidth, 14));
        phaseLabel.setAlignmentY(0.5f);
        east.add(phaseLabel);
        panel.add(east, BorderLayout.EAST);

        return panel;
    }

    private Color barColor(NodeRow row) {
        if (row.state == JIPipeProgressNodeState.FAILED) {
            return colorFail;
        }
        if (row.state == JIPipeProgressNodeState.OK || (row.progressMax > 0 && row.progressValue >= row.progressMax)) {
            return colorOk;
        }
        return colorAccent;
    }

    private static int progressPercent(NodeRow row) {
        return Math.max(0, Math.min(100, (int) Math.round(100.0 * row.progressValue / row.progressMax)));
    }

    private Component renderDetailRow(JList<? extends Row> list, DetailRow row) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(true);
        Color background = colorDetailBackground;
        Color levelColor = colorFaint;
        String levelText = "";
        String message;
        long ts;
        if (row.record instanceof JIPipeProgressRecord.Log log) {
            ts = log.ts();
            JIPipeLogLevel level = levelOf(log.lv());
            levelText = level.name();
            levelColor = switch (level) {
                case INFO -> colorFaint;
                case DEBUG -> mix(colorFaint, colorRowBackground, 0.35f);
                case WARN -> colorWarn;
                case ERROR -> colorFail;
            };
            if (level == JIPipeLogLevel.WARN) {
                background = mix(colorRowBackground, colorWarn, 0.07f);
            } else if (level == JIPipeLogLevel.ERROR) {
                background = mix(colorRowBackground, colorFail, 0.07f);
            }
            message = log.m() == null ? "" : log.m().replace("\r", "").replace("\n", "  ");
        } else if (row.record instanceof JIPipeProgressRecord.Error error) {
            ts = error.ts();
            levelText = "ERROR";
            levelColor = colorFail;
            background = mix(colorRowBackground, colorFail, 0.07f);
            message = error.c() + (error.m() == null || error.m().isEmpty() ? "" : ": " + error.m());
        } else if (row.record instanceof JIPipeProgressRecord.Notification notification) {
            // Run-level user-facing notifications render as warning-tinted entries (R22)
            ts = notification.ts();
            levelText = "WARN";
            levelColor = colorWarn;
            background = mix(colorRowBackground, colorWarn, 0.07f);
            message = notification.h() + (notification.d() == null || notification.d().isEmpty() ? "" : ": " + notification.d());
        } else {
            ts = 0;
            message = row.record.type();
        }
        panel.setBackground(background);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, mix(colorLine, background, 0.5f)),
                BorderFactory.createEmptyBorder(0, 8, 0, 10)));

        JPanel west = new JPanel();
        west.setOpaque(false);
        west.setLayout(new BoxLayout(west, BoxLayout.X_AXIS));
        JLabel tsLabel = new JLabel(TIME_FORMAT.format(Instant.ofEpochMilli(ts)));
        tsLabel.setFont(progressTextFont);
        tsLabel.setForeground(colorFaint);
        west.add(tsLabel);
        west.add(Box.createHorizontalStrut(8));
        JLabel levelLabel = new JLabel(levelText);
        levelLabel.setFont(levelLabel.getFont().deriveFont(10.5f));
        levelLabel.setForeground(levelColor);
        levelLabel.setPreferredSize(new Dimension(42, 14));
        levelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        west.add(levelLabel);
        panel.add(west, BorderLayout.WEST);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(messageLabel.getFont().deriveFont(12.0f));
        messageLabel.setForeground(colorText);
        panel.add(messageLabel, BorderLayout.CENTER);

        if (row.hasStack()) {
            JLabel stackToggle = new JLabel(row.stackOpen() ? "Hide stack trace ▴" : "Show stack trace ▾");
            stackToggle.setFont(stackToggle.getFont().deriveFont(11.0f));
            stackToggle.setForeground(colorAccent);
            stackToggle.setBackground(mix(colorRowBackground, colorAccent, 0.12f));
            stackToggle.setOpaque(true);
            stackToggle.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
            panel.add(stackToggle, BorderLayout.EAST);
        }
        return panel;
    }

    private Component renderStackRow(StackRow row) {
        JLabel label = new JLabel(row.line());
        label.setFont(progressTextFont);
        label.setForeground(colorStackForeground);
        label.setBackground(colorStackBackground);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        return label;
    }

    /**
     * The empty-virtual-row render: same background/separator as a detail row so a detail block
     * stays visually continuous, with a faint marker text.
     */
    private Component renderVirtualRow(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(11.0f));
        label.setForeground(color);
        label.setBackground(colorDetailBackground);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, mix(colorLine, colorDetailBackground, 0.5f)),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        return label;
    }

    private Component renderActionRow(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(11.5f));
        label.setForeground(color);
        label.setBackground(colorDetailBackground);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorLine),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return label;
    }

    /**
     * Returns the level of a numeric log level value (unknown values fall down to the nearest known level).
     */
    private static JIPipeLogLevel levelOf(int value) {
        if (value <= JIPipeLogLevel.DEBUG.value()) {
            return JIPipeLogLevel.DEBUG;
        } else if (value <= JIPipeLogLevel.INFO.value()) {
            return JIPipeLogLevel.INFO;
        } else if (value <= JIPipeLogLevel.WARN.value()) {
            return JIPipeLogLevel.WARN;
        } else {
            return JIPipeLogLevel.ERROR;
        }
    }

    /**
     * The inline progress bar of a node row (rounded track + fill, like the mockup's {@code .pbar}).
     */
    private static class ProgressBar extends JComponent {
        private int value;
        private int max;
        private Color fill;

        void setValue(int value, int max, Color fill) {
            this.value = value;
            this.max = max;
            this.fill = fill;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(110, 9);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (max <= 0) {
                // No determinate progress: no bar is painted (the segment width is still reserved
                // so the phase/status column aligns across rows)
                return;
            }
            Graphics2D graphics2D = (Graphics2D) g.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            graphics2D.setPaint(mixTrack());
            graphics2D.fill(new RoundRectangle2D.Float(0, 0, width, height, height, height));
            if (max > 0 && value > 0) {
                int fillWidth = (int) Math.min(width, Math.round(width * (value / (double) max)));
                if (fillWidth > 0) {
                    graphics2D.setPaint(fill);
                    graphics2D.fill(new RoundRectangle2D.Float(0, 0, Math.max(height, fillWidth), height, height, height));
                }
            }
            graphics2D.dispose();
        }

        private Color mixTrack() {
            // Dim track derived from the fill color (keeps the bar readable in dark themes)
            return new Color(
                    fill.getRed() / 4 + 192,
                    fill.getGreen() / 4 + 192,
                    fill.getBlue() / 4 + 192);
        }
    }
}
