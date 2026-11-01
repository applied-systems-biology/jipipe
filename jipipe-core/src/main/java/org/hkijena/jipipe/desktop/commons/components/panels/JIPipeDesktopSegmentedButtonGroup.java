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

package org.hkijena.jipipe.desktop.commons.components.panels;

import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A Bootstrap-style <em>merged</em> button group: a single-select row of
 * flat, borderless segments inside one rounded outline, the selected segment
 * filled with the theme's primary color. Unlike a {@link JTabbedPane} (which
 * carries a tab strip plus a content area and inflates the header row it
 * lives in) or {@link JIPipeDesktopToggleButtonPanel} (separate bordered
 * buttons with gaps), this is a compact single-row control of a fixed height
 * ({@link #SEGMENT_HEIGHT_PX}) that sits flush next to a text field.
 * <p>
 * Single-select semantics come from the underlying {@link ButtonGroup}. What
 * this control adds on top: the first added segment is the initial selection
 * (silently, no listener fire), out-of-bounds programmatic selection clamps,
 * and selection listeners that fire exactly once per real index change.
 */
public class JIPipeDesktopSegmentedButtonGroup extends JPanel {

    /**
     * The fixed height of a segment row, chosen to sit flush with a standard
     * control (e.g. the 28px search field).
     */
    public static final int SEGMENT_HEIGHT_PX = 28;
    private static final int CORNER_RADIUS = 5;
    private static final int FILL_INSET = 1;

    private final ButtonGroup buttonGroup = new ButtonGroup();
    private final List<JToggleButton> buttons = new ArrayList<>();
    /**
     * The original foreground of each button, restored when it is deselected
     * (the selected segment switches to the primary color's contrast color).
     */
    private final List<Color> originalForegrounds = new ArrayList<>();
    private final List<Consumer<Integer>> selectionListeners = new ArrayList<>();
    /**
     * The currently selected segment, or {@code -1} while none is added yet.
     */
    private int selectedIndex = -1;

    public JIPipeDesktopSegmentedButtonGroup() {
        // Equal-width segments, no gaps: the merged look comes from the
        // container's outline and the buttons' absence of borders.
        setLayout(new GridLayout(1, 0, 0, 0));
        setOpaque(false);
        setFocusable(false);
        setMinimumSize(new Dimension(0, SEGMENT_HEIGHT_PX));
    }

    /**
     * Adds a segment with a text title.
     *
     * @param title the segment's title
     */
    public void addSegment(String title) {
        addSegment(title, null);
    }

    /**
     * Adds a segment with a text title and an optional icon. The first
     * segment added becomes the (silent) selection; later segments are
     * added unselected.
     *
     * @param title the segment's title
     * @param icon  the segment's icon, or {@code null}
     */
    public void addSegment(String title, Icon icon) {
        JToggleButton button = new JToggleButton(title, icon);
        // The modern theme installs a JIPipeDesktopModernToggleButtonUI
        // globally that paints a grey rounded rect on every SELECTED toggle
        // button (ignoring contentAreaFilled) - which would overlay this
        // control's own primary-color fill. The segment buttons use the plain
        // basic UI (whose pressed/selected paint is a no-op) so only this
        // container paints the selected segment.
        button.setUI(new BasicToggleButtonUI());
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        // Inset-only padding (not painted) so the widest label does not run
        // into the fill's edge.
        button.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        button.getModel().setSelected(buttons.isEmpty());
        button.addActionListener(e -> onSegmentActivated(button));
        buttonGroup.add(button);
        buttons.add(button);
        originalForegrounds.add(button.getForeground());
        if (buttons.size() == 1) {
            // Initial selection: silent, no listeners fire.
            selectedIndex = 0;
            applySelectionForeground();
        }
        add(button);
        revalidate();
        repaint();
    }

    /**
     * @return the number of segments
     */
    public int getSegmentCount() {
        return buttons.size();
    }

    /**
     * @param index the segment index
     * @return the segment's button
     */
    public JToggleButton getSegmentButton(int index) {
        return buttons.get(index);
    }

    /**
     * @return the index of the selected segment, or {@code -1} when no
     * segment has been added yet
     */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /**
     * Selects the segment at the given index. Out-of-bounds indexes are
     * clamped to the segment range; re-selecting the current segment is a
     * no-op. Selection listeners fire exactly once per real change, with the
     * new index.
     *
     * @param index the segment to select
     */
    public void setSelectedIndex(int index) {
        if (buttons.isEmpty()) {
            return;
        }
        int clamped = Math.max(0, Math.min(index, buttons.size() - 1));
        if (clamped == selectedIndex) {
            return;
        }
        buttons.get(clamped).setSelected(true); // the group deselects the previous one
        selectedIndex = clamped;
        applySelectionForeground();
        repaint();
        for (Consumer<Integer> listener : selectionListeners) {
            listener.accept(clamped);
        }
    }

    /**
     * Registers a listener that is invoked with the new segment index
     * whenever the selection actually changes.
     *
     * @param listener the listener
     */
    public void addSelectionListener(Consumer<Integer> listener) {
        selectionListeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     *
     * @param listener the listener
     */
    public void removeSelectionListener(Consumer<Integer> listener) {
        selectionListeners.remove(listener);
    }

    /**
     * A mouse click on a segment. The {@link ButtonGroup} guarantees exactly
     * one selected button at all times (re-clicking the selected segment
     * keeps it selected), so this only tracks the index and reports a change
     * when it actually moved.
     */
    private void onSegmentActivated(JToggleButton button) {
        int index = buttons.indexOf(button);
        if (index != selectedIndex) {
            selectedIndex = index;
            applySelectionForeground();
            repaint();
            for (Consumer<Integer> listener : selectionListeners) {
                listener.accept(index);
            }
        }
    }

    /**
     * The selected segment's text switches to the primary color's contrast
     * color; unselected segments keep their original foreground. (Theme
     * changes require a restart, so caching at construction is safe.)
     */
    private void applySelectionForeground() {
        Color selectedForeground = ColorUtils.getContrastColor(ThemeUtils.getCurrentStyle().getPrimaryColor());
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setForeground(i == selectedIndex ? selectedForeground : originalForegrounds.get(i));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        size.height = SEGMENT_HEIGHT_PX;
        return size;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (buttons.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // The single merged outline around the whole group.
            g2.setColor(ThemeUtils.getCurrentStyle().getBorderColor());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS, CORNER_RADIUS);
            // The selected segment's fill, inset from the outline. Corners
            // are only rounded where the group's outline is rounded (first
            // segment: left, last segment: right) so adjacent fills meet
            // flush.
            if (selectedIndex >= 0) {
                Rectangle r = buttons.get(selectedIndex).getBounds();
                int x = r.x + FILL_INSET;
                int y = r.y + FILL_INSET;
                int w = r.width - 2 * FILL_INSET;
                int h = r.height - 2 * FILL_INSET;
                boolean first = selectedIndex == 0;
                boolean last = selectedIndex == buttons.size() - 1;
                g2.setColor(ThemeUtils.getCurrentStyle().getPrimaryColor());
                g2.fill(selectedSegmentPath(x, y, w, h, first, last));
            }
        } finally {
            g2.dispose();
        }
    }

    private static Path2D selectedSegmentPath(int x, int y, int w, int h, boolean roundLeft, boolean roundRight) {
        int cr = CORNER_RADIUS - FILL_INSET;
        Path2D path = new Path2D.Double();
        path.moveTo(x + (roundLeft ? cr : 0), y);
        if (roundLeft) {
            path.quadTo(x, y, x, y + cr);
        }
        path.lineTo(x, y + h - (roundLeft ? cr : 0));
        if (roundLeft) {
            path.quadTo(x, y + h, x + cr, y + h);
        }
        path.lineTo(x + w - (roundRight ? cr : 0), y + h);
        if (roundRight) {
            path.quadTo(x + w, y + h, x + w, y + h - cr);
        }
        path.lineTo(x + w, y + (roundRight ? cr : 0));
        if (roundRight) {
            path.quadTo(x + w, y, x + w - cr, y);
        }
        path.closePath();
        return path;
    }
}
