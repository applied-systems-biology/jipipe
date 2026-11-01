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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The settings tab's home page, shown when no section is selected (the tab
 * opens to it, and so does every scope switch): a <b>Common</b> grid of
 * frequently used settings and an <b>All</b> list of every section of the
 * active scope, grouped by category - each entry a link with a one-line
 * description. Replaces the former "No settings selected" placeholder.
 * <p>
 * The Common entries are addressed by STABLE SHEET IDs (the same discipline
 * as the deep links); each is resolved against the live model at render
 * time - in the active scope first, then by bare id, because two of the
 * entries (hardware acceleration, connected services) are RUNTIME-scoped
 * while the tab may be showing APPLICATION. Clicking an entry that belongs
 * to another scope still lands on it: the tab switches the scope.
 * <p>
 * The visual structure (grid columns, insets, row layout) is verified by
 * the user in the app, per the project's no-Swing-structure-test policy;
 * the selection behavior is exercised by the tab's own wiring (a Common
 * click goes through the same select path as a tree click).
 */
public class SettingsOverviewPanel extends JPanel {

    /**
     * A Common entry: the stable sheet id plus the one-line description
     * shown on its card (name and icon come from the live section).
     */
    record CommonEntry(String sheetId, String blurb) {
    }

    /**
     * The frequently used settings, in display order (2-column grid).
     */
    private static final List<CommonEntry> COMMON_ENTRIES = List.of(
            new CommonEntry("org.hkijena.jipipe:desktop-appearance", "Theme, text sizes, color scheme and file dialogs"),
            new CommonEntry("org.hkijena.jipipe:auto-save", "Periodically save the open project while it changes"),
            new CommonEntry("org.hkijena.jipipe:backups", "Keep backups of projects when saving"),
            new CommonEntry("org.hkijena.jipipe:extensions", "Enable or disable JIPipe extensions"),
            new CommonEntry("org.hkijena.jipipe:hardware-acceleration", "GPU acceleration backends installed on this machine"),
            new CommonEntry("org.hkijena.jipipe:default-environments", "Environments and their default configuration")
    );

    private final SettingsTreeModel model;
    private final SettingsScope scope;
    private final Function<SettingsTreeModel.SheetNode, Icon> iconFor;
    private final Consumer<SettingsTreeModel.SheetNode> onSelect;

    /**
     * @param model     the live section model (names, descriptions, scopes)
     * @param scope     the active scope (its sections fill the All list)
     * @param iconFor   resolves a section's icon (the tab's sheet icons)
     * @param onSelect  called with the clicked section (the tab opens it; a
     *                  cross-scope section switches the scope)
     */
    public SettingsOverviewPanel(SettingsTreeModel model, SettingsScope scope,
                                 Function<SettingsTreeModel.SheetNode, Icon> iconFor,
                                 Consumer<SettingsTreeModel.SheetNode> onSelect) {
        this.model = model;
        this.scope = scope;
        this.iconFor = iconFor;
        this.onSelect = onSelect;

        setLayout(new BorderLayout());
        setOpaque(false);
        JScrollPane scroll = new JScrollPane(buildContent(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        // The default wheel increment is 1px per notch (probed) - far too
        // slow for a page of this length; 25 is the app-wide convention
        // (graph editor, data tracer, code editor).
        scroll.getVerticalScrollBar().setUnitIncrement(25);
        add(scroll, BorderLayout.CENTER);
    }

    private JComponent buildContent() {
        // GridBagLayout (not BoxLayout): every entry spans the full width,
        // the two grid columns divide it evenly, and the All rows stretch -
        // a BoxLayout sizes each child to its preferred width, which leaves
        // a short first section and a centered, width-limited All list.
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(UIUtils.createEmptyBorder(12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;
        content.add(header("Common"), withGbc(gbc, row++, 0, 6));
        content.add(commonGrid(), withGbc(gbc, row++, 4, 4));
        content.add(header("All settings"), withGbc(gbc, row++, 14, 6));
        for (SettingsTreeModel.CategoryNode category : model.categories(scope)) {
            if (category.sheets().isEmpty()) {
                continue;
            }
            JLabel categoryLabel = new JLabel(category.name());
            categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.PLAIN,
                    ThemeUtils.getCurrentStyle().getFontSizeSmall()));
            categoryLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
            content.add(categoryLabel, withGbc(gbc, row++, 10, 2));
            for (SettingsTreeModel.SheetNode sheet : category.sheets()) {
                content.add(allRow(sheet), withGbc(gbc, row++, 0, 0));
            }
        }
        return content;
    }

    private static GridBagConstraints withGbc(GridBagConstraints base, int row, int top, int bottom) {
        GridBagConstraints gbc = (GridBagConstraints) base.clone();
        gbc.gridy = row;
        gbc.insets = new Insets(top, 0, bottom, 0);
        return gbc;
    }

    private static JLabel header(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeLarge()));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JComponent commonGrid() {
        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        for (CommonEntry entry : COMMON_ENTRIES) {
            // Resolve the entry in its own scope first, then by bare id: a
            // Common entry may live in another scope than the one this
            // overview was built for (e.g. the RUNTIME-scoped hardware
            // acceleration shown from the APPLICATION view).
            SettingsTreeModel.SheetNode sheet = model.nodeById(scope, entry.sheetId());
            if (sheet == null) {
                sheet = model.nodeById(entry.sheetId());
            }
            if (sheet == null) {
                continue; // the extension providing it is not active
            }
            grid.add(commonCard(sheet, entry));
        }
        return grid;
    }

    private JComponent commonCard(SettingsTreeModel.SheetNode sheet, CommonEntry entry) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setOpaque(true);
        card.setBorder(new RoundedLineBorder(UIUtils.getControlBorderColor(), 1, 6));
        card.setBackground(UIManager.getColor("Panel.background"));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JLabel icon = new JLabel(iconFor.apply(sheet));
        icon.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 2));
        card.add(icon, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(sheet.name());
        title.setFont(title.getFont().deriveFont(Font.BOLD, ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel blurb = new JLabel(entry.blurb());
        blurb.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
        blurb.setFont(blurb.getFont().deriveFont(ThemeUtils.getCurrentStyle().getFontSizeSmall()));
        blurb.setAlignmentX(LEFT_ALIGNMENT);
        text.add(title);
        text.add(blurb);
        card.add(text, BorderLayout.CENTER);

        card.addMouseListener(clickable(card, () -> onSelect.accept(sheet)));
        return card;
    }

    /**
     * A click listener with a <b>pressed</b> state: while the mouse button is
     * down inside the component it is tinted darker (a transparent component
     * becomes briefly opaque), and restores on release/exit. No hover state -
     * only the press gives feedback, per the requested behavior.
     *
     * @param target the component to tint
     * @param click  what a full click does
     */
    private static MouseAdapter clickable(JComponent target, Runnable click) {
        final Color base = target.getBackground();
        final boolean baseOpaque = target.isOpaque();
        final Color pressed = ColorUtils.mix(base, Color.BLACK, 0.15);
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (target.contains(e.getPoint())) {
                    target.setOpaque(true);
                    target.setBackground(pressed);
                    target.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                restore();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                restore();
            }

            private void restore() {
                target.setOpaque(baseOpaque);
                target.setBackground(base);
                target.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                click.run();
            }
        };
    }

    private JComponent allRow(SettingsTreeModel.SheetNode sheet) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBackground(UIManager.getColor("Panel.background"));
        row.setBorder(UIUtils.createEmptyBorder(3));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel icon = new JLabel(iconFor.apply(sheet));
        icon.setBorder(UIUtils.createEmptyBorder(2));
        row.add(icon, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(sheet.name());
        title.setAlignmentX(LEFT_ALIGNMENT);
        text.add(title);
        if (sheet.description() != null && !sheet.description().isBlank()) {
            JLabel description = new JLabel(sheet.description());
            description.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
            description.setFont(description.getFont().deriveFont(ThemeUtils.getCurrentStyle().getFontSizeSmall()));
            description.setAlignmentX(LEFT_ALIGNMENT);
            text.add(description);
        }
        row.add(text, BorderLayout.CENTER);

        row.addMouseListener(clickable(row, () -> onSelect.accept(sheet)));
        return row;
    }
}
