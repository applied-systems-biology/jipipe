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

package org.hkijena.jipipe.desktop.commons.components.renderers;

import org.hkijena.jipipe.api.nodes.JIPipeNodeClassification;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.utils.ThemeUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The node <em>state label system</em>: small rounded chips (the same visual
 * language as the framework / spatial chips in the node browser) that make a
 * node's provenance and health explicit in the list and tree UIs.
 * <p>
 * Three orthogonal states are surfaced, all read from the entry (so the same
 * helper works in the flat list, the function tree, and the node finder):
 * <ul>
 * <li><b>imported</b> — {@link JIPipeNodeClassification#AutoImport} nodes
 * (the auto-discovered IJ2/CLIJ surface); informational tone;</li>
 * <li><b>unstable</b> — {@link JIPipeNodeDatabaseEntry#isUnstable()} nodes
 * (the {@code @MarkNodeAsUnstable} / node-info flag); danger tone;</li>
 * <li><b>deprecated</b> — {@link JIPipeNodeDatabaseEntry#isDeprecated()}
 * nodes; warning tone.</li>
 * </ul>
 * The states are generalized node metadata (classification + flags), not
 * node-id heuristics, so any node that declares one of them is labelled the
 * same way and — via {@link #isDownRanked(JIPipeNodeDatabaseEntry)} — ordered
 * below the standard curated nodes.
 */
public final class JIPipeNodeStateChips {

    private static final int CHIP_ARC = 6;

    private JIPipeNodeStateChips() {
    }

    /**
     * Whether the entry is down-ranked in the result lists (placed after the
     * standard curated nodes): it is an auto-imported or edge-case node, or
     * it is marked deprecated or unstable. This is the single generalized
     * ordering predicate — no node-id or plugin heuristics.
     *
     * @param entry the entry to classify
     * @return {@code true} when the entry belongs below the standard nodes
     */
    public static boolean isDownRanked(JIPipeNodeDatabaseEntry entry) {
        if (entry.getNodeClassification() != JIPipeNodeClassification.Standard) {
            return true;
        }
        return entry.isDeprecated() || entry.isUnstable();
    }

    /**
     * A single-row panel of the entry's state chips (imported / unstable /
     * deprecated, in that order; an empty panel when the node is standard).
     * The panel is non-opaque so it composes onto any row background.
     *
     * @param entry the entry to label
     * @return a panel with zero or more chips, ready to add to a row
     */
    public static JPanel panel(JIPipeNodeDatabaseEntry entry) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        panel.setOpaque(false);
        var style = ThemeUtils.getCurrentStyle();
        if (entry.getNodeClassification() == JIPipeNodeClassification.AutoImport) {
            panel.add(chip("imported", style.getInfoColor()));
        }
        if (entry.isUnstable()) {
            panel.add(chip("unstable", style.getDangerColor()));
        }
        if (entry.isDeprecated()) {
            panel.add(chip("deprecated", style.getWarningColor()));
        }
        return panel;
    }

    private static JPanel chip(String text, Color color) {
        JPanel chip = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CHIP_ARC, CHIP_ARC);
                g2d.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 9f));
        chip.add(label);
        return chip;
    }
}
