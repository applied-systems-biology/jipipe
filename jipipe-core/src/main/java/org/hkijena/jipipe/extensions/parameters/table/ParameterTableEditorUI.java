/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.parameters.table;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterGeneratorUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIParameterTypeRegistry;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * UI for {@link ParameterTable}
 */
public class ParameterTableEditorUI extends JIPipeParameterEditorUI {

    private JXTable table;
    private Point currentSelection = new Point();
    private JIPipeParameterEditorUI currentEditor;
    private JPopupMenu generatePopupMenu;
    private JPopupMenu replacePopupMenu;

    /**
     * Creates new instance
     *
     * @param workbench       workbench
     * @param parameterAccess Parameter
     */
    public ParameterTableEditorUI(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
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

        JButton addButton = new JButton("Add row", UIUtils.getIconFromResources("actions/list-add.png"));
        addButton.setToolTipText("Adds a new row to the table. It contains the default values.");
        addButton.addActionListener(e -> addRow());
        toolBar.add(addButton);

        JButton generateButton = new JButton("Generate rows", UIUtils.getIconFromResources("actions/list-add.png"));
        generateButton.setToolTipText("Generates new rows and adds them to the table. You can select one column to generate data for.\n" +
                "The other columns contain default values.");
        generatePopupMenu = UIUtils.addPopupMenuToComponent(generateButton);
        toolBar.add(generateButton);

        JButton replaceButton = new JButton("Replace cells", UIUtils.getIconFromResources("actions/edit.png"));
        replaceButton.setToolTipText("Replaces the selected cells with generated values. You have to select cells of one specific column.");
        replacePopupMenu = UIUtils.addPopupMenuToComponent(replaceButton);
        toolBar.add(replaceButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton removeButton = new JButton(UIUtils.getIconFromResources("actions/delete.png"));
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

    private void reloadReplacePopupMenu() {
        replacePopupMenu.removeAll();
        if (table.getSelectedRowCount() == 0) {
            JMenuItem noItem = new JMenuItem("Please select cells of one column");
            noItem.setEnabled(false);
            replacePopupMenu.add(noItem);
            return;
        }
        if (table.getSelectedColumnCount() > 1) {
            JMenuItem noItem = new JMenuItem("Please select only cells of one column");
            noItem.setEnabled(false);
            replacePopupMenu.add(noItem);
            return;
        }
        int col = table.getSelectedColumn();
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        boolean hasColumnEntries = false;
        if (parameterTable != null) {
            for (Class<? extends JIPipeParameterGeneratorUI> generator : JIPipeUIParameterTypeRegistry.getInstance()
                    .getGeneratorsFor(parameterTable.getColumn(col).getFieldClass())) {
                JIPipeDocumentation documentation = JIPipeUIParameterTypeRegistry.getInstance().getGeneratorDocumentationFor(generator);
                JMenuItem replaceCellItem = new JMenuItem(documentation.name());
                replaceCellItem.setToolTipText(documentation.description());
                replaceCellItem.setIcon(UIUtils.getIconFromResources("actions/edit.png"));
                replaceCellItem.addActionListener(e -> replaceColumnValues(col, generator));

                replacePopupMenu.add(replaceCellItem);
                hasColumnEntries = true;
            }
        }

        if (!hasColumnEntries) {
            JMenuItem noItem = new JMenuItem("Nothing to generate");
            noItem.setEnabled(false);
            replacePopupMenu.add(noItem);
        }
    }

    private void replaceColumnValues(int column, Class<? extends JIPipeParameterGeneratorUI> generator) {
        if (table.getSelectedRowCount() == 0 || table.getSelectedColumnCount() > 1) {
            return;
        }

        List<Object> generatedObjects = JIPipeParameterGeneratorUI.showDialog(this, getWorkbench(), generator);
        if (generatedObjects == null)
            return;

        ParameterTable parameterTable = getParameter(ParameterTable.class);
        int[] rows = getSelectedRows(false);
        for (int i = 0; i < Math.min(generatedObjects.size(), rows.length); ++i) {
            parameterTable.setValueAt(generatedObjects.get(i), rows[i], column);
        }
    }

    private void reloadGeneratePopupMenu() {
        generatePopupMenu.removeAll();
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        boolean hasColumnEntries = false;
        if (parameterTable != null) {

            for (int col = 0; col < parameterTable.getColumnCount(); ++col) {
                JMenu columnMenu = new JMenu(parameterTable.getColumn(col).getName());
                columnMenu.setIcon(UIUtils.getIconFromResources("data-types/parameters.png"));

                for (Class<? extends JIPipeParameterGeneratorUI> generator : JIPipeUIParameterTypeRegistry.getInstance()
                        .getGeneratorsFor(parameterTable.getColumn(col).getFieldClass())) {
                    JIPipeDocumentation documentation = JIPipeUIParameterTypeRegistry.getInstance().getGeneratorDocumentationFor(generator);
                    JMenuItem generateRowItem = new JMenuItem(documentation.name());
                    generateRowItem.setToolTipText(documentation.description());
                    generateRowItem.setIcon(UIUtils.getIconFromResources("actions/list-add.png"));
                    int finalCol = col;
                    generateRowItem.addActionListener(e -> generateRow(finalCol, generator));

                    columnMenu.add(generateRowItem);
                }

                if (columnMenu.getItemCount() > 0) {
                    generatePopupMenu.add(columnMenu);
                    hasColumnEntries = true;
                }
            }
        }

        if (!hasColumnEntries) {
            JMenuItem noItem = new JMenuItem("Nothing to generate");
            noItem.setEnabled(false);
            generatePopupMenu.add(noItem);
        }
    }

    private void generateRow(int columnIndex, Class<? extends JIPipeParameterGeneratorUI> generator) {
        List<Object> generatedObjects = JIPipeParameterGeneratorUI.showDialog(this, getWorkbench(), generator);
        if (generatedObjects != null) {
            ParameterTable parameterTable = getParameter(ParameterTable.class);
            for (Object generatedObject : generatedObjects) {
                parameterTable.addRow();
                parameterTable.setValueAt(generatedObject, parameterTable.getRowCount() - 1, columnIndex);
            }
        }
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
                ParameterTable parameterTable = getParameter(ParameterTable.class);
                ParameterTableCellAccess access = new ParameterTableCellAccess(getParameterAccess(), parameterTable,
                        currentSelection.x, currentSelection.y);
                currentEditor = JIPipeUIParameterTypeRegistry.getInstance().createEditorFor(getWorkbench(), access);
                add(currentEditor, BorderLayout.SOUTH);
            }
        }
        reloadReplacePopupMenu();
    }

    /**
     * Returns selected rows as sorted array of model indices
     *
     * @return selected rows in model indices
     */
    private int[] getSelectedRows(boolean sort) {
        int[] displayRows = table.getSelectedRows();
        for (int i = 0; i < displayRows.length; ++i) {
            displayRows[i] = table.getRowSorter().convertRowIndexToModel(displayRows[i]);
        }
        if (sort)
            Arrays.sort(displayRows);
        return displayRows;
    }

    private void removeSelectedRows() {
        int[] selectedRows = getSelectedRows(true);
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        for (int i = selectedRows.length - 1; i >= 0; --i) {
            parameterTable.removeRow(selectedRows[i]);
        }
    }

    private void addRow() {
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        if (parameterTable != null) {
            parameterTable.addRow();
        }
    }

    @Subscribe
    public void onTableStructureChangedEvent(TableModelEvent event) {
        TableModel model = table.getModel();
        table.setModel(new DefaultTableModel());
        table.setModel(model);
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        if (table.getModel() instanceof ParameterTable) {
            ((ParameterTable) table.getModel()).getEventBus().unregister(this);
        }

        ParameterTable parameterTable = getParameter(ParameterTable.class);
        if (parameterTable == null) {
            table.setModel(new DefaultTableModel());
        } else {
            table.setModel(parameterTable);
            parameterTable.getEventBus().register(this);
        }
        reloadGeneratePopupMenu();
        reloadReplacePopupMenu();
    }
}
