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
        add(new JScrollPane(buildContent(),
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    }

    private JComponent buildContent() {
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(UIUtils.createEmptyBorder(12));

        content.add(header("Common"));
        content.add(Box.createVerticalStrut(6));
        content.add(commonGrid());
        content.add(Box.createVerticalStrut(18));

        content.add(header("All " + scopeLabel(scope)));
        for (SettingsTreeModel.CategoryNode category : model.categories(scope)) {
            if (category.sheets().isEmpty()) {
                continue;
            }
            content.add(Box.createVerticalStrut(12));
            JLabel categoryLabel = new JLabel(category.name());
            categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.PLAIN,
                    ThemeUtils.getCurrentStyle().getFontSizeSmall()));
            categoryLabel.setForeground(ThemeUtils.getCurrentStyle().getTextMuted());
            categoryLabel.setAlignmentX(LEFT_ALIGNMENT);
            content.add(categoryLabel);
            for (SettingsTreeModel.SheetNode sheet : category.sheets()) {
                content.add(allRow(sheet));
            }
        }
        return content;
    }

    private static String scopeLabel(SettingsScope scope) {
        switch (scope) {
            case APPLICATION:
                return "application";
            case PROJECT:
                return "project";
            case RUNTIME:
                return "runtime";
            default:
                return scope.name().toLowerCase();
        }
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
        card.setOpaque(false);
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

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSelect.accept(sheet);
            }
        });
        return card;
    }

    private JComponent allRow(SettingsTreeModel.SheetNode sheet) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
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

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSelect.accept(sheet);
            }
        });
        return row;
    }
}
