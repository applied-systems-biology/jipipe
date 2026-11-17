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

package org.hkijena.jipipe.desktop.app.automation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionService;
import org.hkijena.jipipe.api.automation.permissions.AutomationPermissionState;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.plugins.automation.settings.MCPPermissionStates;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The per-tool permission table of the MCP settings sheet: one row per operation id from
 * the automation operation registry, a dropdown (Default, Allow, Ask, Deny, Hide) per row.
 * <p>
 * An edit writes a copy of the states through the parameter access (so the parameter
 * change events and the settings bridge fire) and applies it to the live permission
 * service immediately. "Default" clears the stored state: the category default applies
 * (read-only operations: Allow, others: Ask). The Default entries are displayed with
 * their resolved category default ("Default (Allow)" / "Default (Ask)"); the stored
 * value stays the cleared-state sentinel.
 */
public class JIPipeDesktopMCPPermissionsEditorUI extends JIPipeDesktopParameterEditorUI<MCPPermissionStates> {

    private static final String DEFAULT_LABEL = "Default";
    private static final String DEFAULT_ALLOW_LABEL = "Default (Allow)";
    private static final String DEFAULT_ASK_LABEL = "Default (Ask)";

    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Operation", "Permission"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1;
        }
    };
    /**
     * Guards the change listener against the editor's own rebuilds.
     */
    private boolean reloading;

    public JIPipeDesktopMCPPermissionsEditorUI(InitializationParameters initializationParameters) {
        super(MCPPermissionStates.class, initializationParameters);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JTable table = new JTable(model);
        // The established data-table row height of the codebase (all data table UIs
        // use it as their non-preview default, same as the keyboard shortcuts
        // section); the Swing default is too dense for the dropdown column.
        table.setRowHeight(25);
        TableColumn permissionColumn = table.getColumnModel().getColumn(1);
        permissionColumn.setCellRenderer(new PermissionStateCellRenderer());
        JComboBox<String> stateBox = new JComboBox<>(new String[]{DEFAULT_LABEL,
                AutomationPermissionState.Allow.name(), AutomationPermissionState.Ask.name(),
                AutomationPermissionState.Deny.name(), AutomationPermissionState.Hide.name()});
        PermissionStateCellEditor editor = new PermissionStateCellEditor(stateBox);
        stateBox.setRenderer(new PermissionStateComboRenderer(editor));
        permissionColumn.setCellEditor(editor);
        model.addTableModelListener(e -> apply());

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JLabel hint = new JLabel("Operations without a selection use their category default "
                + "(read-only operations: Allow, others: Ask).");
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(hint, BorderLayout.SOUTH);
    }

    @Override
    public boolean isUIInline() {
        return false;
    }

    @Override
    public void reload() {
        reloading = true;
        try {
            MCPPermissionStates states = getParameter();
            List<String> ids = new ArrayList<>(JIPipe.getAutomation().getOperationRegistry().getIds());
            Collections.sort(ids);
            model.setRowCount(0);
            for (String id : ids) {
                String stored = states.get(id);
                model.addRow(new Object[]{id, stored != null ? stored : DEFAULT_LABEL});
            }
        } finally {
            reloading = false;
        }
    }

    /**
     * Writes the table content through the parameter access and applies it to the live
     * permission service.
     */
    private void apply() {
        if (reloading) {
            return;
        }
        MCPPermissionStates updated = new MCPPermissionStates(getParameter());
        AutomationPermissionService service = JIPipe.getAutomation().getPermissionService();
        for (int row = 0; row < model.getRowCount(); row++) {
            String operationId = String.valueOf(model.getValueAt(row, 0));
            String value = String.valueOf(model.getValueAt(row, 1));
            if (DEFAULT_LABEL.equals(value)) {
                updated.set(operationId, null);
                service.setUserState(operationId, null);
            } else {
                updated.set(operationId, value);
                try {
                    service.setUserState(operationId, AutomationPermissionState.valueOf(value));
                } catch (IllegalArgumentException ignored) {
                    // Unknown label: keep the persisted map untouched for this row.
                }
            }
        }
        setParameter(updated, true);
    }

    /**
     * Returns the displayed label of a stored cell value: the Default entry is decorated
     * with the category default of the operation ("Default (Allow)" for read-only
     * operations, "Default (Ask)" otherwise). Operations that are no longer in the
     * registry keep the plain label. Only the display is decorated; the stored value
     * stays the plain cleared-state sentinel.
     */
    private static String displayLabel(String value, String operationId) {
        if (value == null || !DEFAULT_LABEL.equals(value)) {
            return value;
        }
        AutomationOperation operation = operationId != null
                ? JIPipe.getAutomation().getOperationRegistry().get(operationId) : null;
        if (operation == null) {
            return DEFAULT_LABEL;
        }
        return operation.isReadOnly() ? DEFAULT_ALLOW_LABEL : DEFAULT_ASK_LABEL;
    }

    /**
     * Displays the permission column: the Default entry is decorated with the category
     * default of the row's operation. The model keeps holding the plain stored values.
     */
    private static class PermissionStateCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object id = table.getModel().getValueAt(row, 0);
            setText(displayLabel(value != null ? String.valueOf(value) : null,
                    id != null ? String.valueOf(id) : null));
            return this;
        }
    }

    /**
     * The dropdown editor of the permission column: resolves the edited row's operation
     * id so the combo renderer can decorate the Default entry. The combo items stay the
     * plain stored values, so the selected display text never leaks into the persisted
     * map (the table writes the selected item, which is always a plain item).
     */
    private static class PermissionStateCellEditor extends DefaultCellEditor {

        private String currentOperationId;

        private PermissionStateCellEditor(JComboBox<String> stateBox) {
            super(stateBox);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                                                     int row, int column) {
            Object id = table.getModel().getValueAt(row, 0);
            currentOperationId = id != null ? String.valueOf(id) : null;
            return super.getTableCellEditorComponent(table, value, isSelected, row, column);
        }
    }

    /**
     * Renders the combo items (popup and closed editor state): the Default entry is
     * decorated with the category default of the edited row's operation.
     */
    private static class PermissionStateComboRenderer extends DefaultListCellRenderer {

        private final PermissionStateCellEditor editor;

        private PermissionStateComboRenderer(PermissionStateCellEditor editor) {
            this.editor = editor;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(displayLabel(value != null ? String.valueOf(value) : null, editor.currentOperationId));
            return label;
        }
    }
}
