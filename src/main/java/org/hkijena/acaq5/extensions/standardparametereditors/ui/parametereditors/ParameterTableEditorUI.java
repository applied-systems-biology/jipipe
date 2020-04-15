package org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ParameterTable;
import org.hkijena.acaq5.api.parameters.ParameterTableCellAccess;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;
import org.scijava.Context;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Objects;

/**
 * UI for {@link org.hkijena.acaq5.api.parameters.ParameterTable}
 */
public class ParameterTableEditorUI extends ACAQParameterEditorUI {

    private JXTable table;
    private Point currentSelection = new Point();
    private ACAQParameterEditorUI currentEditor;

    /**
     * Creates new instance
     *
     * @param context         SciJava context
     * @param parameterAccess Parameter
     */
    public ParameterTableEditorUI(Context context, ACAQParameterAccess parameterAccess) {
        super(context, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());

        // Create toolbar for adding/removing rows
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        add(toolBar, BorderLayout.NORTH);

        JButton addButton = new JButton("Add", UIUtils.getIconFromResources("add.png"));
        addButton.addActionListener(e -> addRow());
        toolBar.add(addButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        removeButton.addActionListener(e -> removeSelectedRows());
        toolBar.add(removeButton);

        // Create table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        table = new JXTable();
        table.setCellSelectionEnabled(true);
        table.getSelectionModel().addListSelectionListener(e -> onTableCellSelected());
        table.getColumnModel().getSelectionModel().addListSelectionListener(e -> onTableCellSelected());
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(table, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);
    }

    private void onTableCellSelected() {
        Point selection = new Point(table.getSelectedRow(), table.getSelectedColumn());
        if (!Objects.equals(selection, currentSelection)) {
            currentSelection = selection;
            if (currentEditor != null) {
                remove(currentEditor);
                revalidate();
                repaint();
                currentEditor = null;
            }
            if (currentSelection.x != -1 && currentSelection.y != -1) {
                ParameterTable parameterTable = getParameterAccess().get();
                ParameterTableCellAccess access = new ParameterTableCellAccess(getParameterAccess(), parameterTable,
                        currentSelection.x, currentSelection.y);
                currentEditor = ACAQUIParametertypeRegistry.getInstance().createEditorFor(getContext(), access);
                add(currentEditor, BorderLayout.SOUTH);
            }
        }
    }

    private void removeSelectedRows() {
        int[] selectedRows = table.getSelectedRows();
        ParameterTable parameterTable = getParameterAccess().get();
        for (int i = selectedRows.length - 1; i >= 0; --i) {
            parameterTable.removeRow(i);
        }
    }

    private void addRow() {
        ParameterTable parameterTable = getParameterAccess().get();
        if (parameterTable != null) {
            parameterTable.addRow();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        ParameterTable parameterTable = getParameterAccess().get();
        if (parameterTable == null) {
            table.setModel(new DefaultTableModel());
        } else {
            table.setModel(parameterTable);
        }
    }
}
