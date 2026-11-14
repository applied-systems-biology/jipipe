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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.settings.SettingsBinding;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutAction;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutBinding;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutBinding.Source;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutProfile;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutProfiles;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutRegistry;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutSettings;
import org.hkijena.jipipe.api.shortcuts.KeymapSettings;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopSegmentedButtonGroup;
import org.hkijena.jipipe.desktop.commons.components.search.JIPipeDesktopSearchTextField;
import org.hkijena.jipipe.desktop.commons.dialogs.Button;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The merged <em>Keyboard</em> section of the settings tab (issue #1346, Task
 * 9). Replaces the keyboard placeholder that used to show an empty state; it is
 * hosted by the {@code org.hkijena.jipipe:desktop-keyboard} sheet exactly the
 * way the Appearance and Accessibility sections are hosted (see
 * {@link JIPipeDesktopAppearanceSection#registerCompositions()} and
 * {@link JIPipeDesktopSettingsTab#rebuildRightPane()}).
 * <p>
 * <b>Header.</b> A preset picker over the bundled
 * {@link JIPipeShortcutProfiles#bundled()} presets (selecting a preset applies
 * it one-way and writes the result through the keymap binding), a chord-timeout
 * spinner (milliseconds; {@code 0} waits indefinitely for the second stroke of
 * a chord), and a "Reset section" button that clears every override and
 * restores the default timeout. A "Modified" badge is shown while the keymap
 * deviates from a pristine state (any override, a non-default timeout, or an
 * active profile).
 * <p>
 * <b>Table.</b> One row per action in
 * {@link JIPipeShortcutRegistry#all()}, grouped by category in a fixed order
 * (then any other categories alphabetically). Columns: Action (name), Binding
 * (the effective variants, empty when unassigned), a State badge (Modified /
 * Default / Unassigned), an Edit button, and a per-row Reset button (enabled
 * only while the action has an override). A search field filters the rows by
 * action name.
 * <p>
 * <b>Edit flow.</b> Edit opens {@link JIPipeDesktopKeyboardCaptureDialog}; on a
 * committed chord the section checks
 * {@link JIPipeShortcutRegistry#findConflicts(String, List, KeymapSettings)} and,
 * when the chord collides with another action in the same scope family, shows
 * a warning naming the conflicting actions with "Assign anyway" / Cancel before
 * writing the override through the binding. A cleared (empty) chord never
 * conflicts.
 * <p>
 * <b>Sync.</b> The section subscribes to
 * {@link JIPipeShortcutSettings#binding()} and to the shortcut registry's
 * change listener, so the table reflects changes made from anywhere (another
 * window, the raw settings editor, a settings-file reload) as well as its own
 * writes. It unsubscribes both in {@link #dispose()}, which the hosting tab
 * calls before re-creating the section.
 */
public class JIPipeDesktopKeyboardSection extends JPanel implements JIPipeDesktopWorkbenchAccess, Disposable {

    /**
     * The fixed display order of the action categories (issue #1346). Categories
     * not listed here are appended, alphabetically, after these.
     */
    private static final List<String> CATEGORY_ORDER = List.of(
            "Graph editor tools", "Selection", "Creation", "Input/output", "Layers",
            "Running", "Navigation", "Editor", "Menus", "Dock panels");

    /**
     * The upper bound offered by the chord-timeout spinner, in milliseconds.
     */
    private static final long TIMEOUT_MAX_MS = 30_000L;

    /**
     * The "Assign anyway" button of the conflict warning (identity-compared).
     */
    private static final Button ASSIGN_ANYWAY = Button.custom("Assign anyway");

    /**
     * Table column indices.
     */
    private static final int COL_ACTION = 0;
    private static final int COL_BINDING = 1;
    private static final int COL_STATE = 2;
    private static final int COL_EDIT = 3;
    private static final int COL_RESET = 4;

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeDesktopSettingsTab settingsTab;
    private final SettingsBinding<KeymapSettings> binding;
    private final JIPipeShortcutRegistry registry;

    private final JIPipeDesktopSegmentedButtonGroup presetGroup = new JIPipeDesktopSegmentedButtonGroup();
    private final JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(
            (int) KeymapSettings.DEFAULT_CHORD_TIMEOUT_MS, 0, (int) TIMEOUT_MAX_MS, 100));
    private final JLabel modifiedBadge = new JLabel("Modified");
    private final JIPipeDesktopSearchTextField searchField = new JIPipeDesktopSearchTextField();
    private final ShortcutTableModel tableModel = new ShortcutTableModel();
    private final JTable table = new JTable(tableModel);

    /**
     * The keymap change listener, held as a stable instance so
     * {@link #dispose()} can unsubscribe the exact registration.
     */
    private final Consumer<KeymapSettings> keymapListener = this::onKeymapChanged;
    /**
     * The registry change listener (dock actions register at panel-add time).
     */
    private final Runnable registryListener = this::onRegistryChanged;

    private boolean updatingPresetSelection;
    private boolean updatingTimeout;
    private boolean disposed;

    /**
     * @param workbench   the workbench
     * @param settingsTab the hosting tab (kept for symmetry with the appearance
     *                    section; the keyboard section does not rebuild it)
     */
    public JIPipeDesktopKeyboardSection(JIPipeDesktopWorkbench workbench, JIPipeDesktopSettingsTab settingsTab) {
        this.workbench = workbench;
        this.settingsTab = settingsTab;
        this.binding = JIPipeShortcutSettings.binding();
        this.registry = JIPipeShortcutRegistry.getInstance();
        initialize();
        binding.subscribe(keymapListener);
        registry.addChangeListener(registryListener);
        refreshTable();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        setOpaque(false);
        setBorder(UIUtils.createEmptyBorder(5));
        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
        initializeTable();
    }

    // ------------------------------------------------------------------
    // Header: title, description, presets, timeout, reset
    // ------------------------------------------------------------------

    private JComponent createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel titlePanel = new JPanel(new BorderLayout(8, 0));
        titlePanel.setOpaque(false);
        JLabel nameLabel = new JLabel("Keyboard", JIPipe.RESOURCES.getIcon16("actions/keyboard.png"), JLabel.LEFT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, nameLabel.getFont().getSize2D() * 1.2f));
        titlePanel.add(nameLabel, BorderLayout.WEST);
        titlePanel.add(modifiedBadge, BorderLayout.CENTER);
        header.add(titlePanel, BorderLayout.NORTH);

        JLabel descriptionLabel = new JLabel("Keyboard shortcuts and chord behavior");
        descriptionLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        header.add(descriptionLabel, BorderLayout.CENTER);

        modifiedBadge.setIcon(JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png"));
        modifiedBadge.setForeground(UIManager.getColor("Label.disabledForeground"));
        modifiedBadge.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        header.add(createControls(), BorderLayout.SOUTH);
        return header;
    }

    /**
     * The header control row: the preset picker on the left, the chord-timeout
     * spinner and the section reset on the right.
     */
    private JComponent createControls() {
        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setOpaque(false);

        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        presetPanel.setOpaque(false);
        presetPanel.add(new JLabel("Preset:"));
        for (JIPipeShortcutProfile profile : JIPipeShortcutProfiles.bundled()) {
            presetGroup.addSegment(profile.name());
        }
        // The initial selection is set before the listener is attached, so it
        // does not (re-)apply a preset. refreshTable() keeps it in sync.
        presetGroup.setSelectedIndex(presetIndexFor(JIPipeShortcutSettings.current().getActiveProfileId()));
        presetGroup.addSelectionListener(idx -> {
            if (updatingPresetSelection) {
                return;
            }
            applyPreset(idx);
        });
        presetPanel.add(presetGroup);
        controls.add(presetPanel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        timeoutSpinner.setToolTipText("Maximum time between the two presses of a chord, in milliseconds (0 waits indefinitely)");
        timeoutSpinner.addChangeListener(e -> {
            if (updatingTimeout) {
                return;
            }
            KeymapSettings s = binding.get();
            s.setChordTimeoutMs(((Number) timeoutSpinner.getValue()).longValue());
            binding.set(s);
        });
        right.add(new JLabel("Chord timeout:"));
        right.add(timeoutSpinner);
        JButton resetButton = UIUtils.createButton("Reset section",
                "Clear every shortcut override and restore the default chord timeout",
                JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"),
                this::doResetSection);
        right.add(resetButton);
        controls.add(right, BorderLayout.EAST);
        return controls;
    }

    // ------------------------------------------------------------------
    // Content: search + action table
    // ------------------------------------------------------------------

    private JComponent createContent() {
        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setOpaque(false);

        JPanel searchPanel = new JPanel(new BorderLayout(4, 0));
        searchPanel.setOpaque(false);
        searchField.setToolTipText("Filter shortcuts by action name");
        searchPanel.add(searchField, BorderLayout.WEST);
        // Re-filter the rows as the user types (debounced by the search field).
        searchField.addActionListener(e -> refreshOnEdt());
        content.add(searchPanel, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private void initializeTable() {
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        table.getColumnModel().getColumn(COL_ACTION).setCellRenderer(new ActionCellRenderer());
        table.getColumnModel().getColumn(COL_BINDING).setCellRenderer(new BindingCellRenderer());
        table.getColumnModel().getColumn(COL_STATE).setCellRenderer(new StateChipRenderer());
        table.getColumnModel().getColumn(COL_EDIT).setCellRenderer(new EditButtonRenderer());
        table.getColumnModel().getColumn(COL_RESET).setCellRenderer(new ResetButtonRenderer());

        // Button renderers are visual only (a shared JButton renderer cannot
        // carry per-row action listeners): the clicked column and row are
        // resolved from the event point, exactly like the server monitor's stop
        // button. A double-click on an action row also edits it.
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e) || disposed) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewColumn = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewColumn < 0) {
                    return;
                }
                int modelRow = table.convertRowIndexToModel(viewRow);
                int modelColumn = table.convertColumnIndexToModel(viewColumn);
                JIPipeShortcutAction action = tableModel.rowAction(modelRow);
                if (action == null) {
                    return; // a category header row
                }
                if (e.getClickCount() == 2 && modelColumn == COL_ACTION) {
                    editAction(action);
                } else if (e.getClickCount() == 1 && modelColumn == COL_EDIT) {
                    editAction(action);
                } else if (e.getClickCount() == 1 && modelColumn == COL_RESET
                        && binding.get().hasOverride(action.id())) {
                    resetAction(action);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Table model
    // ------------------------------------------------------------------

    /**
     * One row of the action table: either a category group header or a single
     * shortcut action with its resolved state.
     */
    sealed interface TableRow permits CategoryRow, ActionRow {
    }

    /**
     * A category group header row (renders the category name as a header, the
     * remaining cells empty).
     */
    record CategoryRow(String category) implements TableRow {
    }

    /**
     * One shortcut action row with its effective binding state.
     *
     * @param action      the registered action
     * @param variants    the effective binding variants (empty when unassigned)
     * @param source      where the variants come from
     * @param hasOverride whether the action carries a user override
     */
    record ActionRow(JIPipeShortcutAction action, List<List<KeyStroke>> variants, Source source, boolean hasOverride)
            implements TableRow {
    }

    private class ShortcutTableModel extends AbstractTableModel {
        private final List<TableRow> rows = new ArrayList<>();

        void setRows(List<TableRow> newRows) {
            rows.clear();
            rows.addAll(newRows);
            fireTableDataChanged();
        }

        TableRow rowAt(int rowIndex) {
            return rows.get(rowIndex);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_ACTION -> "Action";
                case COL_BINDING -> "Binding";
                case COL_STATE -> "State";
                case COL_EDIT, COL_RESET -> "";
                default -> "";
            };
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case COL_STATE -> Source.class;
                case COL_EDIT, COL_RESET -> JComponent.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /**
         * @param modelRow a model row index
         * @return the action of the row, or {@code null} for a category header row
         */
        JIPipeShortcutAction rowAction(int modelRow) {
            TableRow row = rows.get(modelRow);
            return row instanceof ActionRow action ? action.action() : null;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TableRow row = rows.get(rowIndex);
            if (row instanceof CategoryRow category) {
                return switch (columnIndex) {
                    case COL_ACTION -> category.category();
                    default -> "";
                };
            }
            ActionRow action = (ActionRow) row;
            return switch (columnIndex) {
                case COL_ACTION -> action.action().name();
                case COL_BINDING -> action.variants().isEmpty() ? "" : JIPipeShortcutRegistry.formatVariants(action.variants());
                case COL_STATE -> action.source();
                case COL_EDIT, COL_RESET -> null; // rendered as buttons by the cell renderers
                default -> null;
            };
        }
    }

    // ------------------------------------------------------------------
    // Refresh (from the keymap or the registry)
    // ------------------------------------------------------------------

    private void onKeymapChanged(KeymapSettings ignored) {
        refreshOnEdt();
    }

    private void onRegistryChanged() {
        refreshOnEdt();
    }

    private void refreshOnEdt() {
        if (SwingUtilities.isEventDispatchThread()) {
            refreshTable();
        } else {
            SwingUtilities.invokeLater(this::refreshTable);
        }
    }

    /**
     * Re-resolves every action against the current keymap, re-groups by
     * category, applies the search filter, and updates the header controls.
     * Must run on the EDT.
     */
    private void refreshTable() {
        if (disposed) {
            return;
        }
        KeymapSettings settings = binding.get();
        JIPipeShortcutRegistry registry = JIPipeShortcutRegistry.getInstance();
        String query = searchField.getText();

        List<TableRow> rows = new ArrayList<>();
        String lastCategory = null;
        for (JIPipeShortcutAction action : categoryOrdered(registry.all())) {
            if (!matchesSearch(action.name(), query)) {
                continue;
            }
            if (!action.category().equals(lastCategory)) {
                lastCategory = action.category();
                rows.add(new CategoryRow(lastCategory));
            }
            JIPipeShortcutBinding effective = registry.bindingFor(action.id(), settings);
            rows.add(new ActionRow(action, effective.variants(), effective.source(), settings.hasOverride(action.id())));
        }
        tableModel.setRows(rows);
        packColumns();

        updatePresetSelection(settings);
        updateTimeoutSpinner(settings);
        updateModifiedBadge(settings);
    }

    /**
     * The actions of {@code all()} (already sorted by category, then name)
     * re-bucketed so the categories appear in {@link #CATEGORY_ORDER}, with any
     * remaining categories appended alphabetically. Package-private so the
     * ordering contract is unit-testable.
     *
     * @param all the registry's actions in its natural order
     * @return the actions re-ordered by the category display order
     */
    static List<JIPipeShortcutAction> categoryOrdered(List<JIPipeShortcutAction> all) {
        Map<String, List<JIPipeShortcutAction>> byCategory = new LinkedHashMap<>();
        for (JIPipeShortcutAction action : all) {
            byCategory.computeIfAbsent(action.category(), k -> new ArrayList<>()).add(action);
        }
        List<String> ordered = new ArrayList<>();
        for (String category : CATEGORY_ORDER) {
            if (byCategory.containsKey(category)) {
                ordered.add(category);
            }
        }
        byCategory.keySet().stream()
                .filter(category -> !CATEGORY_ORDER.contains(category))
                .sorted()
                .forEach(ordered::add);
        List<JIPipeShortcutAction> result = new ArrayList<>();
        for (String category : ordered) {
            result.addAll(byCategory.get(category));
        }
        return result;
    }

    /**
     * @param name  an action name
     * @param query the search text
     * @return whether the name matches the (multi-token) search
     */
    private static boolean matchesSearch(String name, String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return true;
        }
        return name.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT));
    }

    /**
     * Escapes the HTML-significant characters of a plain string, for embedding
     * in the (verbatim-passed) {@code <html>} conflict message.
     *
     * @param text the text to escape
     * @return the escaped text
     */
    private static String escapeHtml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    // ------------------------------------------------------------------
    // Header control sync
    // ------------------------------------------------------------------

    private static int presetIndexFor(String activeProfileId) {
        for (int i = 0; i < JIPipeShortcutProfiles.bundled().size(); i++) {
            if (JIPipeShortcutProfiles.bundled().get(i).id().equals(activeProfileId)) {
                return i;
            }
        }
        return 0;
    }

    private void updatePresetSelection(KeymapSettings settings) {
        updatingPresetSelection = true;
        try {
            presetGroup.setSelectedIndex(presetIndexFor(settings.getActiveProfileId()));
        } finally {
            updatingPresetSelection = false;
        }
    }

    private void applyPreset(int index) {
        if (index < 0 || index >= JIPipeShortcutProfiles.bundled().size()) {
            return;
        }
        JIPipeShortcutProfile profile = JIPipeShortcutProfiles.bundled().get(index);
        KeymapSettings settings = binding.get();
        JIPipeShortcutProfiles.apply(profile, settings);
        settings.setActiveProfileId(profile.id());
        binding.set(settings);
    }

    private void updateTimeoutSpinner(KeymapSettings settings) {
        updatingTimeout = true;
        try {
            // Display-only clamp: a hand-edited settings document may hold a
            // chord timeout outside the spinner's [0, TIMEOUT_MAX_MS] range,
            // and setValue would throw on the EDT during refresh. The stored
            // value is not rewritten.
            long clamped = Math.max(0L, Math.min(TIMEOUT_MAX_MS, settings.getChordTimeoutMs()));
            timeoutSpinner.setValue((int) clamped);
        } finally {
            updatingTimeout = false;
        }
    }

    private void updateModifiedBadge(KeymapSettings settings) {
        // The "Default" preset is the pristine state (it clears all overrides),
        // so it does not count as a modification even though it records an
        // active profile id.
        String activeProfile = settings.getActiveProfileId();
        boolean modified = !settings.getOverrides().isEmpty()
                || settings.getChordTimeoutMs() != KeymapSettings.DEFAULT_CHORD_TIMEOUT_MS
                || (activeProfile != null && !JIPipeShortcutProfiles.DEFAULT.id().equals(activeProfile));
        modifiedBadge.setVisible(modified);
        modifiedBadge.setToolTipText(modified
                ? "This section contains settings that differ from the defaults" : null);
    }

    /**
     * Clears every override, restores the default chord timeout, and clears the
     * active profile, all through the binding.
     */
    private void doResetSection() {
        KeymapSettings settings = binding.get();
        settings.getOverrides().clear();
        settings.setChordTimeoutMs(KeymapSettings.DEFAULT_CHORD_TIMEOUT_MS);
        settings.setActiveProfileId(null);
        binding.set(settings);
    }

    // ------------------------------------------------------------------
    // Edit + per-row reset
    // ------------------------------------------------------------------

    /**
     * Opens the capture dialog for the given action and, on a committed chord,
     * resolves conflicts before writing the override.
     */
    private void editAction(JIPipeShortcutAction action) {
        KeymapSettings current = JIPipeShortcutSettings.current();
        List<List<KeyStroke>> currentVariants =
                JIPipeShortcutRegistry.getInstance().bindingFor(action.id(), current).variants();
        Optional<List<KeyStroke>> captured =
                JIPipeDesktopKeyboardCaptureDialog.capture(this, action.name(), currentVariants);
        if (captured.isEmpty()) {
            return;
        }
        List<KeyStroke> chord = captured.get();
        if (!chord.isEmpty()) {
            KeymapSettings settings = binding.get();
            List<String> conflictIds = registry.findConflicts(action.id(), List.of(chord), settings);
            if (!conflictIds.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (String id : conflictIds) {
                    JIPipeShortcutAction other = registry.byId(id);
                    names.add(escapeHtml(other != null ? other.name() : id));
                }
                // An <html> message passes through the dialog chrome verbatim,
                // so the (already escaped) names are safe to embed.
                String message = "<html>The shortcut you recorded is already assigned to:<br>"
                        + String.join(", ", names) + "<br><br>"
                        + "Assigning it here too would bind the same keys to several actions. "
                        + "Assign it anyway?";
                // asDefault() returns a copy; keep that instance for the
                // identity comparison (the chrome records the exact instance).
                Button assignAnyway = ASSIGN_ANYWAY.asDefault();
                Optional<Button> choice = JIPipeDesktopDialogs.choose(this, "Shortcut conflict", message,
                        assignAnyway, Button.CANCEL);
                if (choice.filter(b -> b == assignAnyway).isEmpty()) {
                    return;
                }
            }
        }
        KeymapSettings settings = binding.get();
        settings.setOverride(action.id(), List.of(chord));
        binding.set(settings);
    }

    /**
     * Resets one action to its registry default (removes its override, if any).
     */
    private void resetAction(JIPipeShortcutAction action) {
        if (!binding.get().hasOverride(action.id())) {
            return;
        }
        KeymapSettings settings = binding.get();
        settings.removeOverride(action.id());
        binding.set(settings);
    }

    // ------------------------------------------------------------------
    // Column packing (derived from content, capped)
    // ------------------------------------------------------------------

    /**
     * Sizes every column to the width of its widest content (header text and
     * rendered cell components, both measured, not assumed), capped so a very
     * long binding cannot blow up the section.
     */
    private void packColumns() {
        javax.swing.table.TableColumnModel columns = table.getColumnModel();
        for (int c = 0; c < table.getColumnCount(); c++) {
            // The header text, measured against the header's own font (a probe
            // label, so this works even before the header is realized).
            JLabel headerProbe = new JLabel(table.getColumnName(c));
            headerProbe.setFont(table.getTableHeader().getFont());
            int width = headerProbe.getPreferredSize().width;
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                TableCellRenderer renderer = table.getCellRenderer(r, c);
                Component comp = renderer.getTableCellRendererComponent(
                        table, tableModel.getValueAt(r, c), false, false, r, c);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            columns.getColumn(c).setPreferredWidth(Math.min(width, 560));
        }
    }

    // ------------------------------------------------------------------
    // Cell renderers
    // ------------------------------------------------------------------

    /**
     * The Action column: a category header row renders as a bold header with a
     * subtle background; an action row renders the name with the description as
     * tooltip.
     */
    private class ActionCellRenderer extends JLabel implements TableCellRenderer {
        ActionCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            TableRow r = tableModel.rowAt(row);
            if (r instanceof CategoryRow category) {
                setText(category.category());
                setFont(getFont().deriveFont(Font.BOLD));
                setForeground(UIManager.getColor("Label.foreground"));
                setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
                setToolTipText(null);
            } else {
                ActionRow action = (ActionRow) r;
                setText(action.action().name());
                setFont(getFont().deriveFont(Font.PLAIN));
                setForeground(UIManager.getColor("Label.foreground"));
                setBackground(selected ? table.getSelectionBackground() : table.getBackground());
                setToolTipText(action.action().description());
            }
            return this;
        }
    }

    /**
     * The Binding column: the effective variants in a monospaced font; empty
     * (blank cell) when the action is unassigned.
     */
    private class BindingCellRenderer extends JLabel implements TableCellRenderer {
        private final Font baseFont = new Font(Font.MONOSPACED, Font.PLAIN, UIManager.getFont("Label.font").getSize());

        BindingCellRenderer() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            TableRow r = tableModel.rowAt(row);
            boolean header = r instanceof CategoryRow;
            setText(header ? "" : (String) value);
            setFont(baseFont);
            setForeground(selected ? table.getSelectionForeground() : UIManager.getColor("Label.foreground"));
            setBackground(selected && !header ? table.getSelectionBackground()
                    : header ? ThemeUtils.getCurrentStyle().getWindowBackground() : table.getBackground());
            setToolTipText(null);
            return this;
        }
    }

    /**
     * The State column: a small filled chip reading Modified / Default /
     * Unassigned.
     */
    private class StateChipRenderer extends JPanel implements TableCellRenderer {
        private static final int CHIP_ARC = 6;
        private final JLabel label = new JLabel();
        private Color chipColor;

        StateChipRenderer() {
            super(new FlowLayout(FlowLayout.LEFT, 4, 0));
            setOpaque(true);
            label.setForeground(Color.WHITE);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 9f));
            add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            TableRow r = tableModel.rowAt(row);
            if (r instanceof CategoryRow) {
                label.setText("");
                setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
                chipColor = null;
            } else {
                Source source = (Source) value;
                var style = ThemeUtils.getCurrentStyle();
                switch (source) {
                    case OVERRIDE -> {
                        label.setText("Modified");
                        chipColor = style.getWarningColor();
                    }
                    case DEFAULT -> {
                        label.setText("Default");
                        chipColor = style.getInfoColor();
                    }
                    default -> {
                        label.setText("Unassigned");
                        chipColor = style.getSecondaryColor();
                    }
                }
                if (selected) {
                    setBackground(table.getSelectionBackground());
                } else {
                    setBackground(table.getBackground());
                }
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (chipColor == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(chipColor);
            int h = label.getPreferredSize().height;
            g2.fillRoundRect(2, (getHeight() - h) / 2, label.getPreferredSize().width + 6, h, CHIP_ARC, CHIP_ARC);
            g2.dispose();
        }
    }

    /**
     * The Edit column: a visual-only button (the click is resolved by the
     * table's mouse listener, so a shared renderer is safe).
     */
    private class EditButtonRenderer extends JButton implements TableCellRenderer {

        EditButtonRenderer() {
            setText("Edit");
            setIcon(JIPipe.RESOURCES.getIcon16("actions/edit.png"));
            setFocusPainted(false);
            setContentAreaFilled(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            TableRow r = tableModel.rowAt(row);
            boolean header = r instanceof CategoryRow;
            setText(header ? "" : "Edit");
            setIcon(header ? null : JIPipe.RESOURCES.getIcon16("actions/edit.png"));
            setEnabled(!header);
            if (selected && !header) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(header ? ThemeUtils.getCurrentStyle().getWindowBackground() : table.getBackground());
                setForeground(UIManager.getColor("Label.foreground"));
            }
            return this;
        }
    }

    /**
     * The Reset column: a visual-only button, enabled only while the row's
     * action has an override (the click is resolved by the table's mouse
     * listener, so a shared renderer is safe).
     */
    private class ResetButtonRenderer extends JButton implements TableCellRenderer {

        ResetButtonRenderer() {
            setText("Reset");
            setIcon(JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"));
            setFocusPainted(false);
            setContentAreaFilled(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column) {
            TableRow r = tableModel.rowAt(row);
            boolean header = r instanceof CategoryRow;
            boolean hasOverride = !header && ((ActionRow) r).hasOverride();
            setText(header ? "" : "Reset");
            setIcon(header ? null : JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"));
            setEnabled(hasOverride);
            setToolTipText(header ? null : hasOverride
                    ? "Reset this shortcut to its default"
                    : "This shortcut has no override");
            if (selected && !header) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(header ? ThemeUtils.getCurrentStyle().getWindowBackground() : table.getBackground());
                setForeground(UIManager.getColor("Label.foreground"));
            }
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Override
    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        binding.unsubscribe(keymapListener);
        registry.removeChangeListener(registryListener);
    }
}
