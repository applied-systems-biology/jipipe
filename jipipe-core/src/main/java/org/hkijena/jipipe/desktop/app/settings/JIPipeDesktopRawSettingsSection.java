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
import org.hkijena.jipipe.api.service.components.JIPipeSettingsServiceComponent;
import org.hkijena.jipipe.api.settings.SettingsScope;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.dialogs.JIPipeDesktopDialogs;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The raw settings editor of the settings tab: every setting of the selected
 * scope in one table (modern settings system, Task 20).
 * <p>
 * It lists <em>every</em> setting of the selected scope: the documented keys and
 * the stored content the section tree has no UI for (pass-through blocks and
 * undocumented sub-paths of known blocks). See {@link RawSettingsTableModel}
 * for the row enumeration and the write semantics.
 * <p>
 * <b>Immediate, no Save button.</b> A double-click opens a JSON text editor for
 * the row (the raw nature of the editor is honest: one text field works for
 * every key type), the model applies the parsed value through
 * {@link JIPipeSettingsServiceComponent#writeWithOrigin} with
 * {@link JIPipeSettingsServiceComponent.ChangeOrigin#RAW_EDIT}, and the engine's
 * debounced auto-save persists it. "Reset" writes a documented key's default
 * ({@link JIPipeSettingsServiceComponent.ChangeOrigin#RESET}) or removes a
 * value without a default (pass-through values are removed).
 * <p>
 * <b>Scopes.</b> The scope selector mirrors the tab's switcher (Application /
 * Project / Runtime) and is initialized with the tab's current scope; switching
 * it here switches the tab as well, so the section tree and the raw table never
 * disagree about which scope is being edited.
 * <p>
 * Registered like a merged section through
 * {@link JIPipeDesktopSettingsTab#registerMergedPage(String, String)} with the
 * {@value #SHEET_ID} anchor sheet, so it appears as a first-class section of
 * the tree.
 */
public class JIPipeDesktopRawSettingsSection extends JPanel implements Disposable {

    /**
     * The registration id of the anchor sheet that hosts this section
     * (persisted in profiles; must never change).
     */
    public static final String SHEET_ID = "org.hkijena.jipipe:desktop-raw-settings";
    /**
     * The merged-section page id of the raw editor.
     */
    public static final String PAGE_RAW = "raw-settings";

    private static final SettingsScope[] SCOPES = {
            SettingsScope.APPLICATION, SettingsScope.PROJECT, SettingsScope.RUNTIME};

    private final JIPipeSettingsServiceComponent settingsService;

    private final JComboBox<SettingsScope> scopeSelector = new JComboBox<>(SCOPES);
    private final JTextField filterField = new JTextField();
    private final RawTableModel tableModel = new RawTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<RawTableModel> sorter = new TableRowSorter<>(tableModel);
    private final JButton editButton = UIUtils.createButton("Edit", JIPipe.RESOURCES.getIcon16("actions/edit.png"), this::editSelectedRow);
    private final JButton resetButton = UIUtils.createButton("Reset", JIPipe.RESOURCES.getIcon16("actions/edit-reset.png"), this::resetSelectedRow);
    private final JLabel statusLabel = new JLabel();

    private final JIPipeSettingsServiceComponent.SettingsChangedEventListener changedListener;

    private RawSettingsTableModel model;
    private SettingsScope scope;
    private boolean disposed;

    /**
     * Creates the section. It lists {@link SettingsScope#APPLICATION} until the
     * hosting tab sets its current scope with {@link #setScope(SettingsScope)}.
     */
    public JIPipeDesktopRawSettingsSection() {
        this.settingsService = JIPipe.getSettingsService();
        if (settingsService == null) {
            throw new IllegalStateException("The settings service is not available");
        }
        this.scope = SettingsScope.APPLICATION;
        this.changedListener = this::onSettingsChanged;
        initialize();
        settingsService.getChangedEventEmitter().subscribe(changedListener);
        reload();
    }

    /**
     * Registers the raw editor with the settings tab: the anchor sheet's form is
     * replaced by this section. Idempotent; called from the plugin registration.
     */
    public static void registerRawSection() {
        JIPipeDesktopSettingsTab.registerMergedPage(SHEET_ID, PAGE_RAW);
    }

    /**
     * Opens the settings tab on the raw editor (e.g. from a hidden power-user
     * action) — the entry point that keeps the section reachable without hunting
     * for it in the tree.
     *
     * @param workbench the workbench
     */
    public static void openRawSection(JIPipeDesktopWorkbench workbench) {
        if (workbench instanceof JIPipeDesktopProjectWorkbench projectWorkbench) {
            projectWorkbench.openSettingsSection(SettingsScope.APPLICATION, SHEET_ID);
        }
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);
        JPanel titlePanel = new JPanel(new BorderLayout(8, 0));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Raw settings", JIPipe.RESOURCES.getIcon16("actions/configure.png"), JLabel.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() * 1.2f));
        titlePanel.add(title, BorderLayout.WEST);

        JPanel scopePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        scopePanel.setOpaque(false);
        scopePanel.add(new JLabel("Scope:"));
        scopeSelector.setPreferredSize(new Dimension(140, 28));
        scopePanel.add(scopeSelector);
        titlePanel.add(scopePanel, BorderLayout.EAST);
        header.add(titlePanel, BorderLayout.NORTH);

        JLabel description = new JLabel("Every setting of the selected scope, including values that have no dedicated editor");
        description.setForeground(UIManager.getColor("Label.disabledForeground"));
        header.add(description, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        add(createWarningBanner(), BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setOpaque(false);
        content.add(createToolBar(), BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        initializeTable();
        initializeListeners();
    }

    private JComponent createWarningBanner() {
        JPanel banner = new JPanel(new BorderLayout(8, 0));
        banner.setBackground(UIManager.getColor("InfoBackground"));
        banner.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JLabel label = new JLabel("<html><strong>Editing raw settings can corrupt your profile.</strong> "
                + "Changes apply immediately and are saved automatically.</html>",
                JIPipe.RESOURCES.getIcon16("emblems/emblem-warning.png"), JLabel.LEFT);
        banner.add(label, BorderLayout.CENTER);
        return banner;
    }

    private JComponent createToolBar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        filterField.setToolTipText("Filter by settings path (case-insensitive substring)");
        filterField.setPreferredSize(new Dimension(320, 28));
        JPanel filterPanel = new JPanel(new BorderLayout(4, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel(JIPipe.RESOURCES.getIcon16("actions/search.png")), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);
        toolbar.add(filterPanel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setOpaque(false);
        buttons.add(editButton);
        buttons.add(resetButton);
        buttons.add(statusLabel);
        toolbar.add(buttons, BorderLayout.EAST);
        return toolbar;
    }

    private void initializeTable() {
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        int[] widths = {420, 260, 90, 120, 80};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.rowAtPoint(e.getPoint()) >= 0) {
                    editSelectedRow();
                }
            }
        });
    }

    private void initializeListeners() {
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        scopeSelector.addActionListener(e -> onScopeSelected());
    }

    private void onSettingsChanged(JIPipeSettingsServiceComponent.SettingsChangedEvent event) {
        if (event.getScope() == scope) {
            // A change reached the listed document, from this section or from a
            // typed editor elsewhere: re-read the rows so the table never shows a
            // stale value.
            SwingUtilities.invokeLater(this::reload);
        }
    }

    /**
     * Reloads the table from the document and re-applies the filter. Called on
     * construction, on scope switch and on external change.
     */
    public void reload() {
        if (disposed) {
            return;
        }
        this.model = new RawSettingsTableModel(settingsService, scope);
        tableModel.setRows(model.rows(filterField.getText()));
        applyFilter();
        updateButtons();
    }

    private void applyFilter() {
        String text = filterField.getText();
        if (text == null || text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            // The filter is the model's own case-insensitive path substring rule.
            String needle = text.trim().toLowerCase(java.util.Locale.ROOT);
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends RawTableModel, ? extends Integer> entry) {
                    return entry.getModel().getRow(entry.getIdentifier()).path()
                            .toLowerCase(java.util.Locale.ROOT).contains(needle);
                }
            });
        }
        updateButtons();
    }

    private void onScopeSelected() {
        SettingsScope selected = (SettingsScope) scopeSelector.getSelectedItem();
        if (selected == null || selected == scope) {
            return;
        }
        this.scope = selected;
        reload();
    }

    private RawSettingsTableModel.RawRow selectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return tableModel.getRow(table.convertRowIndexToModel(viewRow));
    }

    private void updateButtons() {
        RawSettingsTableModel.RawRow row = selectedRow();
        editButton.setEnabled(row != null);
        resetButton.setEnabled(row != null);
        if (row == null) {
            statusLabel.setText(" ");
        } else {
            // A pass-through value has no default: its reset removes it.
            statusLabel.setText(row.isDocumented() ? "Documented key" : "Pass-through value (no default)");
        }
    }

    private void editSelectedRow() {
        RawSettingsTableModel.RawRow row = selectedRow();
        if (row == null || model == null) {
            return;
        }
        String current = model.rawJson(row.path());
        String message = "<html><strong>" + escapeHtml(row.path()) + "</strong>"
                + (row.isDocumented() ? " &mdash; " + escapeHtml(row.typeLabel())
                + (row.description().isEmpty() ? "" : "<br>" + escapeHtml(row.description())) : "")
                + "<br>Edit the value as JSON.</html>";
        String edited = UIUtils.getMultiLineStringByDialog(this, "Edit raw setting", message, current);
        if (edited == null) {
            return;
        }
        try {
            model.setValue(row.path(), edited);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        reload();
        selectPath(row.path());
    }

    private void resetSelectedRow() {
        RawSettingsTableModel.RawRow row = selectedRow();
        if (row == null || model == null) {
            return;
        }
        String question = row.isDocumented()
                ? "Reset '" + row.path() + "' to its default" + (row.defaultText().isEmpty() ? "" : " (" + row.defaultText() + ")") + "?"
                : "Remove the pass-through value at '" + row.path() + "'? It has no default to restore.";
        if (!JIPipeDesktopDialogs.confirm(this, "Reset setting", question)) {
            return;
        }
        model.resetKey(row.path());
        reload();
        selectPath(row.path());
    }

    private void selectPath(String path) {
        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            RawSettingsTableModel.RawRow row = tableModel.getRow(table.convertRowIndexToModel(viewRow));
            if (Objects.equals(row.path(), path)) {
                table.setRowSelectionInterval(viewRow, viewRow);
                return;
            }
        }
    }

    /**
     * @return the scope currently listed
     */
    public SettingsScope getScope() {
        return scope;
    }

    /**
     * Sets the listed scope. The tab calls this when its own scope switcher
     * changes, so the two selectors stay in sync.
     *
     * @param scope the scope to list
     */
    public void setScope(SettingsScope scope) {
        if (scope == null || scope == this.scope) {
            return;
        }
        this.scope = scope;
        // Assigning the selected item fires the action, which reloads; the guard
        // above makes this a no-op when the item already is the target.
        if (scopeSelector.getSelectedItem() == scope) {
            reload();
        } else {
            scopeSelector.setSelectedItem(scope);
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        settingsService.getChangedEventEmitter().unsubscribe(changedListener);
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * The {@link JTable} model over {@link RawSettingsTableModel.RawRow}s.
     */
    private static final class RawTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Path", "Value", "Type", "Default", "Kind"};
        private List<RawSettingsTableModel.RawRow> rows = List.of();

        void setRows(List<RawSettingsTableModel.RawRow> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        RawSettingsTableModel.RawRow getRow(int index) {
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            RawSettingsTableModel.RawRow row = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> row.path();
                case 1 -> row.valuePreview();
                case 2 -> row.typeLabel();
                case 3 -> row.defaultText();
                default -> row.isPassThrough() ? "pass-through" : "key";
            };
        }
    }
}