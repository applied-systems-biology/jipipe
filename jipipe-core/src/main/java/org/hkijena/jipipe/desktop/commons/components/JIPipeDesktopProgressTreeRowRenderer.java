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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.progress.JIPipeLogLevel;
import org.hkijena.jipipe.api.progress.JIPipeProgressNodeState;
import org.hkijena.jipipe.api.progress.JIPipeProgressRecord;
import org.hkijena.jipipe.desktop.commons.components.icons.SpinnerIcon;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders the flat rows of the {@link JIPipeDesktopProgressTreePanel} - the Swing implementation of
 * the v2 design contract ({@code docs/superpowers/specs/2026-11-04-progress-system-overhaul/ui/v2-progress-panel.html}).
 * <p>
 * Rows are plain data snapshots (built by the panel's refresh cycle). Each row kind owns ONE
 * prebuilt component tree that {@link #getListCellRendererComponent} reconfigures per call (the
 * {@code JIPipeDesktopFindPanel.FindTreeCellRenderer} pattern): BasicListUI re-renders EVERY model
 * row on every layout pass ({@code updateLayoutState}), and the panel resets the model on every
 * progress batch, so constructing a fresh component tree per call saturated the EDT during large
 * runs. The renderer is EDT-confined and one row is configured at a time, so reuse is safe; a
 * reused row ends with an explicit invalidate + validate so freshly set text is measured
 * synchronously instead of rendering with the previous row's bounds.
 * <p>
 * Node rows: indent, expander caret, state icon (running spinner / done / failed / cancelled),
 * dim partition chip ({@code P0}), label with a faint {@code [uuid]} suffix when the label carries
 * one, {@code +N} hidden-descendants badge (collapsed only; R29: all descendant nodes under the
 * current filters, R42: excluding finished nodes with no visible records and no rendering
 * descendants), faint {@code <n> debug} chip, inline
 * progress bar with mono {@code v/mx (pct%)} text, and a phase tag (duration / {@code running Ns} /
 * {@code failed}). Detail rows: timestamp + level + message (WARN/ERROR tinted), error rows with a
 * one-line summary and a {@code Show stack trace} toggle, dark monospace stack lines,
 * {@code Show N older records from run data…} and {@code N DEBUG records hidden - switch to Level:
 * DEBUG} action rows.
 */
public class JIPipeDesktopProgressTreeRowRenderer implements ListCellRenderer<JIPipeDesktopProgressTreeRowRenderer.Row> {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int INDENT_PER_LEVEL = 22;

    /**
     * One row of the flat tree list. Sealed: the row types below.
     */
    public sealed interface Row permits NodeRow, DetailRow, StackRow, MoreRow, LoadingRow, PlaceholderRow, FinishedItemsGateRow {
    }

    /**
     * The synthetic "Finished items" gate row (R47): while the run root is RUNNING, the finished
     * children of a RUNNING node collapse into this one row (double-click toggles the reveal).
     * {@code count} is the grouped item count, {@code firstTs}/{@code lastTs} the group's time
     * span for the duration column.
     */
    public record FinishedItemsGateRow(int parentNodeId, int depth, int count, long firstTs, long lastTs,
                                       boolean revealed) implements Row {
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
        private final boolean showProgressBar;
        private final String phaseText;

        public NodeRow(int nodeId, int depth, String partitionChip, String label, String uuidSuffix,
                       JIPipeProgressNodeState state, boolean expandable, boolean expanded,
                       int badgeCount, int debugCount, int progressValue, int progressMax, String phaseText) {
            this(nodeId, depth, partitionChip, label, uuidSuffix, state, expandable, expanded,
                    badgeCount, debugCount, progressValue, progressMax, true, phaseText);
        }

        public NodeRow(int nodeId, int depth, String partitionChip, String label, String uuidSuffix,
                       JIPipeProgressNodeState state, boolean expandable, boolean expanded,
                       int badgeCount, int debugCount, int progressValue, int progressMax,
                       boolean showProgressBar, String phaseText) {
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
            this.showProgressBar = showProgressBar;
            this.phaseText = phaseText;
        }

        /**
         * Whether the row renders its inline progress bar. False for phase rows (R38: machinery
         * progress lands on the ruleless run root, the mockup's phase-row bars predate that ruling).
         */
        public boolean isShowProgressBar() {
            return showProgressBar;
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

        public int getProgressValue() {
            return progressValue;
        }

        public int getProgressMax() {
            return progressMax;
        }

        public String getPhaseText() {
            return phaseText;
        }

        public String getUuidSuffix() {
            return uuidSuffix;
        }

        public int getDepth() {
            return depth;
        }

        public int getBadgeCount() {
            return badgeCount;
        }

        public int getDebugCount() {
            return debugCount;
        }
    }

    /**
     * One record of an expanded node (log message or error summary).
     * <p>
     * {@code slot} is the record's detail slot - the identity used by the virtual detail layer
     * (R36) to cache chunks and to anchor stack traces; the paged mode uses -1.
     */
    public record DetailRow(int nodeId, JIPipeProgressRecord record, boolean hasStack, boolean stackOpen, int slot) implements Row {
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

    private final SpinnerIcon spinnerIcon;

    // Theme snapshot (read once; a theme switch rebuilds the whole UI anyway)
    private final Color colorAccent;
    private final Color colorOk;
    private final Color colorFail;
    private final Color colorWarn;
    private final Color colorBarTrack;
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
     * stable for as long as the font object is the same - which is what aligns the columns across
     * rows. A different font object (user font/size change → the theme reinstalls component fonts)
     * re-derives them once, not per row.
     */
    private Font measuredFont;
    private Font progressTextFont;
    private Font phaseFont;
    private int progressTextColumnWidth;
    private int phaseColumnWidth;

    // The mockup's .pbar track proportions, scaled from the progress text size (190x9 at 11.5px
    // text): the track is about 16.5x the text size wide and 0.78x tall, so bars scale with the
    // user's font instead of a hardcoded pixel width (round-2 geometry ruling)
    private int barTrackWidth;
    private int barTrackHeight;

    // Prebuilt row components (one per row kind), reconfigured per render call. Built once here
    // instead of per call: BasicListUI.updateLayoutState renders every model row on every layout
    // pass, and the panel resets the model on every progress batch, so per-call construction
    // saturated the EDT on large runs. Visibility is the per-call switch for conditional parts
    // (BoxLayout skips invisible children).
    private final JPanel nodePanel = new JPanel(new BorderLayout(6, 0));
    private final JPanel nodeWest = new JPanel();
    private final JLabel nodeCaretLabel = new JLabel();
    private final JLabel nodeStateLabel = new JLabel();
    private final JLabel nodeChipLabel = new JLabel();
    private final JPanel nodeCenter = new JPanel();
    private final JLabel nodeNameLabel = new JLabel();
    private final JLabel nodeUuidLabel = new JLabel();
    private final JPanel nodeEast = new JPanel();
    private final JLabel nodeDebugChip = new JLabel();
    private final Component nodeDebugStrut = Box.createHorizontalStrut(6);
    private final JLabel nodeBadgeLabel = new JLabel();
    private final Component nodeBadgeStrut = Box.createHorizontalStrut(6);
    private final Component nodeChipStrut = Box.createHorizontalStrut(2);
    private final Component nodeUuidStrut = Box.createHorizontalStrut(4);
    private final JPanel nodeProgressSegment = new JPanel();
    private final ProgressBar nodeBar = new ProgressBar();
    private final JLabel nodeProgressTextLabel = new JLabel();
    private final JLabel nodePhaseLabel = new JLabel();
    private final JPanel detailPanel = new JPanel(new BorderLayout(8, 0));
    private final JPanel detailWest = new JPanel();
    private final JLabel detailTsLabel = new JLabel();
    private final JLabel detailLevelLabel = new JLabel();
    private final JLabel detailMessageLabel = new JLabel();
    private final JLabel detailStackToggle = new JLabel();
    private final JLabel stackLabel = new JLabel();
    private final JLabel virtualLabel = new JLabel();
    private final JLabel actionLabel = new JLabel();
    private final JLabel fallbackLabel = new JLabel();
    private final JPanel gatePanel = new JPanel(new BorderLayout(6, 0));
    private final JPanel gateWest = new JPanel();
    private final JLabel gateCaretLabel = new JLabel();
    private final JLabel gateTitleLabel = new JLabel();
    private final JLabel gateCountLabel = new JLabel();
    private final JLabel gateSpanLabel = new JLabel();
    // Resolved lazily on first use (the renderer can be built before JIPipe is instantiated)
    private Icon statusOkIcon;
    private Icon statusErrorIcon;
    private Icon statusCancelledIcon;

    /**
     * Re-derives the font-dependent geometry when the list's font changed (identity check - one
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
        float statusTextSize = progressTextFont.getSize2D();
        barTrackWidth = Math.round(statusTextSize * 190f / 11.5f);
        // At least 7px tall so the rounded track reads as a bar at every font size
        barTrackHeight = Math.max(7, Math.round(statusTextSize * 9f / 11.5f));
        // Widest sensible phase/status text: a run running for a full day in seconds
        phaseColumnWidth = list.getFontMetrics(phaseFont).stringWidth("running 86400s") + 6;
        // The reused rows carry the font-derived geometry: fonts of the fixed columns, the
        // fixed column widths, and the bar track size (one re-derivation per font change,
        // never per row)
        nodeProgressTextLabel.setFont(progressTextFont);
        nodeProgressTextLabel.setPreferredSize(new Dimension(progressTextColumnWidth, 14));
        nodeProgressTextLabel.setMaximumSize(new Dimension(progressTextColumnWidth, 14));
        nodePhaseLabel.setFont(phaseFont);
        nodePhaseLabel.setPreferredSize(new Dimension(phaseColumnWidth, 14));
        nodePhaseLabel.setMaximumSize(new Dimension(phaseColumnWidth, 14));
        nodeBar.setTrackSize(barTrackWidth, barTrackHeight);
        Dimension progressSegmentSize = new Dimension(barTrackWidth + 7 + progressTextColumnWidth,
                Math.max(18, barTrackHeight + 4));
        nodeProgressSegment.setPreferredSize(progressSegmentSize);
        nodeProgressSegment.setMaximumSize(progressSegmentSize);
        detailTsLabel.setFont(progressTextFont);
        stackLabel.setFont(progressTextFont);
        gateSpanLabel.setFont(phaseFont);
        gateSpanLabel.setPreferredSize(new Dimension(phaseColumnWidth, 14));
        gateSpanLabel.setMaximumSize(new Dimension(phaseColumnWidth, 14));
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
        // The bar track: a neutral, slightly darker shade of the row background (the mockup's
        // #e6e8ee on its near-white rows), so it reads as a recessed track in every theme
        colorBarTrack = mix(themeStyle.getPanelBackground(), themeStyle.getTextForeground(), 0.10f);
        colorText = themeStyle.getTextForeground();
        colorDim = themeStyle.getTextMuted();
        colorFaint = mix(colorDim, themeStyle.getPanelBackground(), 0.45f);
        colorLine = mix(themeStyle.getBorderColor(), themeStyle.getPanelBackground(), 0.55f);
        colorRowBackground = themeStyle.getPanelBackground();
        colorDetailBackground = mix(themeStyle.getPanelBackground(), themeStyle.getTextForeground(), 0.035f);
        colorChipBackground = mix(themeStyle.getPanelBackground(), themeStyle.getTextForeground(), 0.07f);
        buildRowComponents();
    }

    /**
     * Assembles the row component trees once: structure, constants (fonts, alignments, fixed
     * borders, struts), and visibility defaults. Per-call state (texts, colors, icons,
     * conditional visibility, the depth indent border) is applied by the configure methods; the
     * font-derived geometry is applied by {@link #updateFontDependentWidths}.
     */
    private void buildRowComponents() {
        // Node row: west (caret + state + partition chip), center (label + uuid suffix),
        // east (debug chip + badge + progress segment + phase column)
        nodePanel.setOpaque(true);
        nodeWest.setOpaque(false);
        nodeWest.setLayout(new BoxLayout(nodeWest, BoxLayout.X_AXIS));
        nodeCaretLabel.setFont(nodeCaretLabel.getFont().deriveFont(9.0f));
        nodeCaretLabel.setPreferredSize(new Dimension(14, 14));
        nodeWest.add(nodeCaretLabel);
        // Fixed gap so the caret triangle and the state icon never touch, at any indent level
        nodeWest.add(Box.createHorizontalStrut(3));
        nodeStateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        nodeStateLabel.setPreferredSize(new Dimension(16, 16));
        nodeWest.add(nodeStateLabel);
        nodeChipLabel.setFont(nodeChipLabel.getFont().deriveFont(10.0f));
        nodeChipLabel.setOpaque(true);
        nodeChipLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        // Align the small chip with the text baseline
        nodeChipLabel.setAlignmentY(0.5f);
        nodeChipLabel.setVisible(false);
        nodeChipStrut.setVisible(false);
        nodeWest.add(nodeChipStrut);
        nodeWest.add(nodeChipLabel);
        nodePanel.add(nodeWest, BorderLayout.WEST);
        nodeCenter.setOpaque(false);
        nodeCenter.setLayout(new BoxLayout(nodeCenter, BoxLayout.X_AXIS));
        nodeCenter.add(nodeNameLabel);
        nodeUuidLabel.setFont(nodeUuidLabel.getFont().deriveFont(nodeUuidLabel.getFont().getSize2D() - 1.5f));
        nodeUuidLabel.setVisible(false);
        nodeUuidStrut.setVisible(false);
        nodeCenter.add(nodeUuidStrut);
        nodeCenter.add(nodeUuidLabel);
        nodePanel.add(nodeCenter, BorderLayout.CENTER);
        nodeEast.setOpaque(false);
        nodeEast.setLayout(new BoxLayout(nodeEast, BoxLayout.X_AXIS));
        nodeDebugChip.setFont(nodeDebugChip.getFont().deriveFont(11.0f));
        nodeDebugChip.setOpaque(true);
        nodeDebugChip.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        nodeDebugChip.setAlignmentY(0.5f);
        nodeDebugChip.setVisible(false);
        nodeDebugStrut.setVisible(false);
        nodeEast.add(nodeDebugChip);
        nodeEast.add(nodeDebugStrut);
        nodeBadgeLabel.setFont(nodeBadgeLabel.getFont().deriveFont(11.0f));
        nodeBadgeLabel.setOpaque(true);
        nodeBadgeLabel.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        nodeBadgeLabel.setAlignmentY(0.5f);
        nodeBadgeLabel.setVisible(false);
        nodeBadgeStrut.setVisible(false);
        nodeEast.add(nodeBadgeLabel);
        nodeEast.add(nodeBadgeStrut);
        // The progress segment always occupies its fixed width (a row without determinate
        // progress leaves it empty - no bar is painted, but the phase/status column does not
        // shift); the v/mx text is right-aligned within its fixed sub-width
        nodeProgressSegment.setOpaque(false);
        nodeProgressSegment.setLayout(new BoxLayout(nodeProgressSegment, BoxLayout.X_AXIS));
        nodeBar.setOpaque(false);
        nodeBar.setTrack(colorBarTrack);
        nodeBar.setAlignmentY(0.5f);
        nodeProgressSegment.add(nodeBar);
        nodeProgressSegment.add(Box.createHorizontalStrut(7));
        nodeProgressTextLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nodeProgressSegment.add(nodeProgressTextLabel);
        nodeProgressSegment.setAlignmentY(0.5f);
        nodeEast.add(nodeProgressSegment);
        // The phase/status text is the last column: fixed (font-derived) width, right-aligned
        // (rows without a phase reserve the same space)
        nodePhaseLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        nodePhaseLabel.setAlignmentY(0.5f);
        nodeEast.add(nodePhaseLabel);
        nodePanel.add(nodeEast, BorderLayout.EAST);

        // Detail row: west (timestamp + level), center (message), east (stack toggle)
        detailPanel.setOpaque(true);
        detailWest.setOpaque(false);
        detailWest.setLayout(new BoxLayout(detailWest, BoxLayout.X_AXIS));
        detailWest.add(detailTsLabel);
        detailWest.add(Box.createHorizontalStrut(8));
        detailLevelLabel.setFont(detailLevelLabel.getFont().deriveFont(10.5f));
        detailLevelLabel.setPreferredSize(new Dimension(42, 14));
        detailLevelLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        detailWest.add(detailLevelLabel);
        detailPanel.add(detailWest, BorderLayout.WEST);
        detailMessageLabel.setFont(detailMessageLabel.getFont().deriveFont(12.0f));
        detailPanel.add(detailMessageLabel, BorderLayout.CENTER);
        detailStackToggle.setFont(detailStackToggle.getFont().deriveFont(11.0f));
        detailStackToggle.setOpaque(true);
        detailStackToggle.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        detailStackToggle.setVisible(false);
        detailPanel.add(detailStackToggle, BorderLayout.EAST);

        // Stack row: a dark monospace line
        stackLabel.setOpaque(true);
        stackLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Virtual row: same background/separator as a detail row so a detail block stays
        // visually continuous, with a faint marker text
        virtualLabel.setHorizontalAlignment(SwingConstants.CENTER);
        virtualLabel.setFont(virtualLabel.getFont().deriveFont(11.0f));
        virtualLabel.setOpaque(true);
        virtualLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, mix(colorLine, colorDetailBackground, 0.5f)),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));

        // Action row ("Show N older records...", "Loading records...")
        actionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        actionLabel.setFont(actionLabel.getFont().deriveFont(11.5f));
        actionLabel.setOpaque(true);
        actionLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorLine),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));

        // Gate row: caret + italic title + count chip, span in the duration column
        gatePanel.setOpaque(true);
        gateWest.setOpaque(false);
        gateWest.setLayout(new BoxLayout(gateWest, BoxLayout.X_AXIS));
        gateCaretLabel.setFont(gateCaretLabel.getFont().deriveFont(9.0f));
        gateCaretLabel.setPreferredSize(new Dimension(14, 14));
        gateWest.add(gateCaretLabel);
        gateWest.add(Box.createHorizontalStrut(3));
        gateTitleLabel.setFont(gateTitleLabel.getFont().deriveFont(Font.ITALIC, gateTitleLabel.getFont().getSize2D()));
        gateWest.add(gateTitleLabel);
        gateWest.add(Box.createHorizontalStrut(6));
        gateCountLabel.setFont(gateCountLabel.getFont().deriveFont(11.0f));
        gateCountLabel.setOpaque(true);
        gateCountLabel.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
        gateWest.add(gateCountLabel);
        gatePanel.add(gateWest, BorderLayout.WEST);
        gateSpanLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gatePanel.add(gateSpanLabel, BorderLayout.EAST);
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
            return configureNodeRow(list, nodeRow, isSelected);
        } else if (value instanceof DetailRow detailRow) {
            return configureDetailRow(list, detailRow, isSelected);
        } else if (value instanceof StackRow stackRow) {
            return configureStackRow(list, stackRow, isSelected);
        } else if (value instanceof MoreRow moreRow) {
            return configureActionRow("Show " + formatCount(moreRow.olderCount()) + " older records from run data…", colorAccent, list, isSelected);
        } else if (value instanceof LoadingRow) {
            return configureActionRow("Loading records…", colorFaint, list, isSelected);
        } else if (value instanceof PlaceholderRow) {
            // Unloaded chunk: a lightweight placeholder row - the chunk load for the surrounding
            // viewport replaces it with the real rows
            return configureVirtualRow("…", colorFaint, list, isSelected);
        } else if (value instanceof FinishedItemsGateRow gateRow) {
            return configureGateRow(list, gateRow, isSelected);
        }
        return fallbackLabel;
    }

    /**
     * The selection colors (R46): the list's LAF selection background/foreground, consistent with
     * the app's other lists.
     */
    private Color selectionBackground(JList<?> list) {
        return list.getSelectionBackground() != null ? list.getSelectionBackground() : colorRowBackground;
    }

    private Color selectionForeground(JList<?> list) {
        return list.getSelectionForeground() != null ? list.getSelectionForeground() : colorText;
    }

    private Component configureNodeRow(JList<? extends Row> list, NodeRow row, boolean isSelected) {
        Color selBackground = isSelected ? selectionBackground(list) : null;
        Color selForeground = isSelected ? selectionForeground(list) : null;
        nodePanel.setBackground(isSelected ? selBackground : colorRowBackground);
        nodePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, colorLine),
                BorderFactory.createEmptyBorder(0, 8 + row.depth * INDENT_PER_LEVEL, 0, 10)));

        // West: caret + state icon + partition chip
        nodeCaretLabel.setText(row.expandable ? (row.expanded ? "▼" : "▶") : "");
        nodeCaretLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.25f) : colorDim);
        if (row.state == JIPipeProgressNodeState.RUNNING) {
            nodeStateLabel.setIcon(spinnerIcon);
        } else {
            // Resource state icons (R33): no UTF8 glyphs (they render inconsistently across OS
            // fonts); the same icons the footer's state label uses. CANCELLED previously fell
            // into the failure glyph branch and now gets its own icon.
            nodeStateLabel.setText("");
            if (statusOkIcon == null) {
                statusOkIcon = JIPipe.RESOURCES.getIcon16(JIPipeDesktopProgressTreePanel.ICON_STATUS_OK);
                statusErrorIcon = JIPipe.RESOURCES.getIcon16(JIPipeDesktopProgressTreePanel.ICON_STATUS_ERROR);
                statusCancelledIcon = JIPipe.RESOURCES.getIcon16(JIPipeDesktopProgressTreePanel.ICON_STATUS_CANCELLED);
            }
            nodeStateLabel.setIcon(switch (row.state) {
                case OK -> statusOkIcon;
                case FAILED -> statusErrorIcon;
                case CANCELLED -> statusCancelledIcon;
                default -> null;
            });
        }

        if (row.partitionChip != null) {
            nodeChipLabel.setText(row.partitionChip);
            nodeChipLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.30f) : colorFaint);
            nodeChipLabel.setBackground(isSelected ? mix(selBackground, selForeground, 0.14f) : colorChipBackground);
            nodeChipLabel.setVisible(true);
            nodeChipStrut.setVisible(true);
        } else {
            nodeChipLabel.setVisible(false);
            nodeChipStrut.setVisible(false);
        }

        // Center: label (+ faint uuid suffix)
        nodeNameLabel.setText(row.label);
        nodeNameLabel.setForeground(isSelected ? selForeground : colorText);
        if (row.uuidSuffix != null) {
            nodeUuidLabel.setText(row.uuidSuffix);
            nodeUuidLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.35f) : colorFaint);
            nodeUuidLabel.setVisible(true);
            nodeUuidStrut.setVisible(true);
        } else {
            nodeUuidLabel.setVisible(false);
            nodeUuidStrut.setVisible(false);
        }

        // East: right-aligned fixed-order segments - [+N badge] [progress bar + v/mx text]
        // [phase/status text] - with stable widths, so bars and the status column align across
        // rows even when some rows lack progress (user report "Align")
        if (row.debugCount > 0) {
            nodeDebugChip.setText(formatCount(row.debugCount) + " debug");
            nodeDebugChip.setForeground(isSelected ? mix(selForeground, selBackground, 0.30f) : colorFaint);
            nodeDebugChip.setBackground(isSelected ? mix(selBackground, selForeground, 0.14f) : colorChipBackground);
            nodeDebugChip.setVisible(true);
            nodeDebugStrut.setVisible(true);
        } else {
            nodeDebugChip.setVisible(false);
            nodeDebugStrut.setVisible(false);
        }

        if (!row.expanded && row.badgeCount > 0) {
            nodeBadgeLabel.setText("+" + formatCount(row.badgeCount));
            nodeBadgeLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.25f) : colorDim);
            nodeBadgeLabel.setBackground(isSelected ? mix(selBackground, selForeground, 0.14f) : colorChipBackground);
            nodeBadgeLabel.setVisible(true);
            nodeBadgeStrut.setVisible(true);
        } else {
            nodeBadgeLabel.setVisible(false);
            nodeBadgeStrut.setVisible(false);
        }

        // R38: a row that must not show a bar renders with max <= 0 (no bar, no v/mx text), while
        // the fixed-width segment keeps the phase/status column aligned across rows
        int barMax = row.showProgressBar ? row.progressMax : 0;
        int barValue = row.showProgressBar ? row.progressValue : 0;
        nodeBar.setProgress(barValue, barMax, barFillFraction(row), barFillFull(row), barFillColor(row));
        nodeProgressTextLabel.setText(barMax > 0
                ? row.progressValue + "/" + row.progressMax + " (" + progressPercent(row) + "%)"
                : "");
        nodeProgressTextLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.25f) : colorDim);

        nodePhaseLabel.setText(row.phaseText == null ? "" : row.phaseText);
        nodePhaseLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.30f) : colorFaint);

        // Force a synchronous re-layout with the freshly set texts and visibilities:
        // JLabel.setText only queues an async revalidate, and CellRendererPane's validate()
        // no-ops on a still-valid container - without this the row renders with the PREVIOUS
        // row's text bounds
        nodePanel.invalidate();
        nodePanel.validate();
        return nodePanel;
    }

    /**
     * The bar fill color, derived from the node state only: the accent color while running,
     * ok-green for a finished node, err-red for a failed one (the mockup's {@code .fill},
     * {@code .fill.done}, and {@code .fill.fail} classes).
     */
    private Color barFillColor(NodeRow row) {
        if (row.state == JIPipeProgressNodeState.FAILED) {
            return colorFail;
        }
        if (row.state == JIPipeProgressNodeState.OK) {
            return colorOk;
        }
        return colorAccent;
    }

    /**
     * The fill fraction of a row's bar: {@code v/mx} clamped to [0,1] (duplicate parallel-loop
     * records can push v past mx). A finished (OK) node paints full regardless of where the
     * counters stopped (the mockup's {@code .fill.done} at width 100%; row-limited runs end below
     * their label total); a FAILED node paints its counter fraction in red (the mockup's failed
     * row: {@code .fill.fail} at width 2% beside "2/100 (2%)").
     */
    private static float barFillFraction(NodeRow row) {
        if (row.state == JIPipeProgressNodeState.OK) {
            return 1f;
        }
        if (row.progressMax <= 0) {
            return 0f;
        }
        return (float) Math.max(0, Math.min(1, row.progressValue / (double) row.progressMax));
    }

    /**
     * Whether the bar renders full-width (a finished node only; a running node that reached its
     * total fills the track through the clamped fraction, in the accent color).
     */
    private static boolean barFillFull(NodeRow row) {
        return row.state == JIPipeProgressNodeState.OK;
    }

    private static int progressPercent(NodeRow row) {
        // Truncated like the mockup ("5/200 (2%)" for 2.5%), never rounded
        return Math.max(0, Math.min(100, (int) (100.0 * row.progressValue / row.progressMax)));
    }

    private Component configureDetailRow(JList<? extends Row> list, DetailRow row, boolean isSelected) {
        Color selBackground = isSelected ? selectionBackground(list) : null;
        Color selForeground = isSelected ? selectionForeground(list) : null;
        Color background = isSelected ? selBackground : colorDetailBackground;
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
        detailPanel.setBackground(background);
        detailPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, mix(colorLine, background, 0.5f)),
                BorderFactory.createEmptyBorder(0, 8, 0, 10)));

        detailTsLabel.setText(TIME_FORMAT.format(Instant.ofEpochMilli(ts)));
        detailTsLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.30f) : colorFaint);
        detailLevelLabel.setText(levelText);
        detailLevelLabel.setForeground(levelColor);

        detailMessageLabel.setText(message);
        detailMessageLabel.setForeground(isSelected ? selForeground : colorText);

        if (row.hasStack()) {
            detailStackToggle.setText(row.stackOpen() ? "Hide stack trace ▴" : "Show stack trace ▾");
            detailStackToggle.setForeground(colorAccent);
            detailStackToggle.setBackground(isSelected
                    ? mix(selBackground, colorAccent, 0.20f)
                    : mix(colorRowBackground, colorAccent, 0.12f));
            detailStackToggle.setVisible(true);
        } else {
            detailStackToggle.setVisible(false);
        }
        // Synchronous re-layout of the freshly set texts (see configureNodeRow)
        detailPanel.invalidate();
        detailPanel.validate();
        return detailPanel;
    }

    private Component configureStackRow(JList<? extends Row> list, StackRow row, boolean isSelected) {
        stackLabel.setText(row.line());
        stackLabel.setForeground(isSelected ? selectionForeground(list) : colorStackForeground);
        stackLabel.setBackground(isSelected ? selectionBackground(list) : colorStackBackground);
        return stackLabel;
    }

    /**
     * The empty-virtual-row render: same background/separator as a detail row so a detail block
     * stays visually continuous, with a faint marker text.
     */
    private Component configureVirtualRow(String text, Color color, JList<? extends Row> list, boolean isSelected) {
        virtualLabel.setText(text);
        virtualLabel.setForeground(isSelected ? selectionForeground(list) : color);
        virtualLabel.setBackground(isSelected ? selectionBackground(list) : colorDetailBackground);
        return virtualLabel;
    }

    private Component configureActionRow(String text, Color color, JList<? extends Row> list, boolean isSelected) {
        actionLabel.setText(text);
        actionLabel.setForeground(isSelected ? selectionForeground(list) : color);
        actionLabel.setBackground(isSelected ? selectionBackground(list) : colorDetailBackground);
        return actionLabel;
    }

    /**
     * The "Finished items" gate row (R47): a collapsed/revealed triangle, an italic label, the
     * grouped count as a +N-style badge, and the group's time span in the duration column (the
     * mockup's gate rows).
     */
    private Component configureGateRow(JList<? extends Row> list, FinishedItemsGateRow row, boolean isSelected) {
        Color selBackground = isSelected ? selectionBackground(list) : null;
        Color selForeground = isSelected ? selectionForeground(list) : null;
        gatePanel.setBackground(isSelected ? selBackground : colorDetailBackground);
        gatePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, mix(colorLine, colorDetailBackground, 0.5f)),
                BorderFactory.createEmptyBorder(0, 8 + row.depth() * INDENT_PER_LEVEL, 0, 10)));

        gateCaretLabel.setText(row.revealed() ? "▼" : "▶");
        gateCaretLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.25f) : colorDim);
        gateTitleLabel.setText("Finished items");
        gateTitleLabel.setForeground(isSelected ? selForeground : colorDim);
        gateCountLabel.setText(formatCount(row.count()));
        gateCountLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.25f) : colorDim);
        gateCountLabel.setBackground(isSelected ? mix(selBackground, selForeground, 0.14f) : colorChipBackground);

        gateSpanLabel.setText(formatDuration(Math.max(0, row.lastTs() - row.firstTs())));
        gateSpanLabel.setForeground(isSelected ? mix(selForeground, selBackground, 0.30f) : colorFaint);
        // Synchronous re-layout of the freshly set texts (see configureNodeRow)
        gatePanel.invalidate();
        gatePanel.validate();
        return gatePanel;
    }

    /**
     * The duration format of the panel's phase column (mirrored: the gate row's span renders in
     * the same column with the same shape).
     */
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        if (seconds < 60) {
            return String.format("%.1fs", millis / 1000.0);
        }
        return String.format("%d:%02d", seconds / 60, seconds % 60);
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
     * The fill fraction is {@code v/mx} clamped to [0,1]; a finished node shows the full bar in
     * its state color (OK green, FAILED red), a running one the accent color - derived from the
     * node state, not from {@code v == mx}, because row-limited runs end below their label total.
     */
    private static final class ProgressBar extends JComponent {
        private static final Color TRACK_INSET_SHADOW = new Color(0, 0, 0, 15);
        private int trackWidth = 1;
        private int trackHeight = 1;
        private Color track = Color.GRAY;
        private int value;
        private int max;
        private float fraction;
        private boolean full;
        private Color fill;

        /**
         * The track geometry: re-applied when the font-derived sizes change (the bar is a
         * long-lived renderer component now, not a per-row construction).
         */
        void setTrackSize(int trackWidth, int trackHeight) {
            this.trackWidth = trackWidth;
            this.trackHeight = trackHeight;
        }

        void setTrack(Color track) {
            this.track = track;
        }

        void setProgress(int value, int max, float fraction, boolean full, Color fill) {
            this.value = value;
            this.max = max;
            this.fraction = fraction;
            this.full = full;
            this.fill = fill;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(trackWidth, trackHeight);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (max <= 0) {
                // Determinate progress only: a row without counters paints nothing
                return;
            }
            Graphics2D graphics2D = (Graphics2D) g.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float width = getWidth();
            float height = getHeight();
            float radius = height;
            // The track: a neutral, slightly darker pill (the mockup's #e6e8ee reads the same in
            // light and dark themes)
            graphics2D.setPaint(track);
            graphics2D.fill(new RoundRectangle2D.Float(0, 0, width, height, radius, radius));
            // The mockup's subtle inset shadow along the track's top edge (inset 0 1px 1px rgba(0,0,0,.06))
            graphics2D.setPaint(TRACK_INSET_SHADOW);
            graphics2D.fill(new Rectangle2D.Float(1f, 0f, Math.max(0f, width - 2f), 1f));
            float fillWidth = full ? width : Math.min(width, width * Math.max(0f, Math.min(1f, fraction)));
            if (fillWidth > 0f) {
                graphics2D.setPaint(fill);
                graphics2D.fill(new RoundRectangle2D.Float(0, 0, Math.max(height, fillWidth), height, radius, radius));
            }
            graphics2D.dispose();
        }
    }
}
