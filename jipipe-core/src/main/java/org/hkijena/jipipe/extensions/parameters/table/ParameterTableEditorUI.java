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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.AddDynamicParameterPanel;
import org.hkijena.jipipe.ui.components.ParameterTreeUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterGeneratorUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

        JButton addRowButton = new JButton("Add row", UIUtils.getIconFromResources("actions/edit-table-insert-row-under.png"));
        addRowButton.setToolTipText("Adds a new row to the table. It contains the default values.");
        addRowButton.addActionListener(e -> addRow());
        toolBar.add(addRowButton);

        JButton addColumnButton = new JButton("Add columns", UIUtils.getIconFromResources("actions/edit-table-insert-column-right.png"));
        addColumnButton.setToolTipText("Adds new columns to the table.");
        JPopupMenu addColumnMenu = UIUtils.addPopupMenuToComponent(addColumnButton);

        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            JMenuItem importColumnFromAlgorithmButton = new JMenuItem("Import from node", UIUtils.getIconFromResources("actions/rabbitvcs-import.png"));
            importColumnFromAlgorithmButton.addActionListener(e -> importColumnFromAlgorithm());
            importColumnFromAlgorithmButton.setToolTipText("Imports a column from an existing node.");
            addColumnMenu.add(importColumnFromAlgorithmButton);
        }
        JMenuItem addCustomColumnButton = new JMenuItem("Custom", UIUtils.getIconFromResources("actions/auto-type.png"));
        addCustomColumnButton.addActionListener(e -> addCustomColumn());
        addCustomColumnButton.setToolTipText("Allows you to enter a custom column.");
        addColumnMenu.add(addCustomColumnButton);

        toolBar.add(addColumnButton);

        JButton generateButton = new JButton("Generate rows", UIUtils.getIconFromResources("actions/autocorrection.png"));
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

        JPopupMenu removeMenu = UIUtils.addPopupMenuToComponent(removeButton);

        JMenuItem removeRowsButton = new JMenuItem("Remove selected rows", UIUtils.getIconFromResources("actions/edit-table-delete-row.png"));
        removeRowsButton.addActionListener(e -> removeSelectedRows());
        removeMenu.add(removeRowsButton);

        JMenuItem removeColumnsButton = new JMenuItem("Remove selected columns", UIUtils.getIconFromResources("actions/edit-table-delete-column.png"));
        removeColumnsButton.addActionListener(e -> removeSelectedColumns());
        removeMenu.add(removeColumnsButton);

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


    private void importColumnFromAlgorithm() {
        Component content = getWorkbench().getDocumentTabPane().getCurrentContent();
        if(getWorkbench() instanceof JIPipeProjectWorkbench) {
            JIPipeGraph graph =((JIPipeProjectWorkbench) getWorkbench()).getProject().getGraph();
            JIPipeParameterTree globalTree = graph.getParameterTree();

            List<Object> importedParameters = ParameterTreeUI.showPickerDialog(getWorkbench().getWindow(), globalTree, "Import parameter");
            for (Object importedParameter : importedParameters) {
                if (importedParameter instanceof JIPipeParameterAccess) {
                    JIPipeParameterTree.Node node = globalTree.getSourceNode(((JIPipeParameterAccess) importedParameter).getSource());
                    importParameterColumn(node, (JIPipeParameterAccess) importedParameter);
                } else if (importedParameter instanceof JIPipeParameterTree.Node) {
                    for (JIPipeParameterAccess access : ((JIPipeParameterTree.Node) importedParameter).getParameters().values()) {
                        if (access.getVisibility().isVisibleIn(JIPipeParameterVisibility.TransitiveVisible)) {
                            JIPipeParameterTree.Node node = globalTree.getSourceNode(access.getSource());
                            importParameterColumn(node, access);
                        }
                    }
                }
            }
        }
        else {
            JOptionPane.showMessageDialog(this, "There is no graph editor open.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importParameterColumn(JIPipeParameterTree.Node node, JIPipeParameterAccess importedParameter) {
        List<String> path = node.getPath();
        path.add(importedParameter.getKey());
        path.remove(0);

        String uniqueKey = String.join("/", path);
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        if(parameterTable.containsColumn(uniqueKey))
            return;
        ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn();
        column.setFieldClass(importedParameter.getFieldClass());
        column.setKey(uniqueKey);
        column.setName(importedParameter.getName());
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.getFieldClass());
        parameterTable.addColumn(column, info.duplicate(importedParameter.get(Object.class)));

        setParameter(parameterTable, true);
        refreshTable();
    }

    private void addCustomColumn() {
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        JIPipeDynamicParameterCollection collection = new JIPipeDynamicParameterCollection();
        collection.setAllowedTypes(JIPipe.getParameterTypes()
                .getRegisteredParameters().values().stream().map(JIPipeParameterTypeInfo::getFieldClass).collect(Collectors.toSet()));
        AddDynamicParameterPanel.showDialog(this, collection);
        for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
            if(parameterTable.containsColumn(entry.getKey()))
                continue;
            ParameterTable.ParameterColumn column = new ParameterTable.ParameterColumn();
            column.setFieldClass(entry.getValue().getFieldClass());
            column.setKey(entry.getKey());
            column.setName(entry.getValue().getName());
            JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(column.getFieldClass());
            parameterTable.addColumn(column, info.newInstance());
        }
        setParameter(parameterTable, true);
        refreshTable();
    }


    private void refreshTable() {
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        table.setModel(new DefaultTableModel());
        table.setModel(parameterTable);
        table.packAll();
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
            for (Class<? extends JIPipeParameterGeneratorUI> generator : JIPipe.getParameterTypes()
                    .getGeneratorsFor(parameterTable.getColumn(col).getFieldClass())) {
                JIPipeDocumentation documentation = JIPipe.getParameterTypes().getGeneratorDocumentationFor(generator);
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

                for (Class<? extends JIPipeParameterGeneratorUI> generator : JIPipe.getParameterTypes()
                        .getGeneratorsFor(parameterTable.getColumn(col).getFieldClass())) {
                    JIPipeDocumentation documentation = JIPipe.getParameterTypes().getGeneratorDocumentationFor(generator);
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
                currentEditor = JIPipe.getParameterTypes().createEditorFor(getWorkbench(), access);
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

    /**
     * Returns selected rows as sorted array of model indices
     *
     * @return selected rows in model indices
     */
    private int[] getSelectedColumns(boolean sort) {
        int[] displayColumns = table.getSelectedColumns();
        for (int i = 0; i < displayColumns.length; ++i) {
            displayColumns[i] = table.convertColumnIndexToModel(displayColumns[i]);
        }
        if (sort)
            Arrays.sort(displayColumns);
        return displayColumns;
    }

    private void removeSelectedRows() {
        int[] selectedRows = getSelectedRows(true);
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        for (int i = selectedRows.length - 1; i >= 0; --i) {
            parameterTable.removeRow(selectedRows[i]);
        }
    }

    private void removeSelectedColumns() {
        int[] selectedColumns = getSelectedColumns(true);
        ParameterTable parameterTable = getParameter(ParameterTable.class);
        for (int i = selectedColumns.length - 1; i >= 0; --i) {
            parameterTable.removeColumn(selectedColumns[i]);
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
