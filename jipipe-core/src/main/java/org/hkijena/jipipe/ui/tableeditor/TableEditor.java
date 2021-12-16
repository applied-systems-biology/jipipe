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

package org.hkijena.jipipe.ui.tableeditor;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.layouts.ModifiedFlowLayout;
import org.hkijena.jipipe.ui.plotbuilder.PlotEditor;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.ScrollableSizeHint;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Spreadsheet UI
 */
public class TableEditor extends JIPipeWorkbenchPanel {
    private static final int MAX_UNDO = 10;
    private ResultsTableData tableModel;
    private JXTable jxTable;
    private Stack<ResultsTableData> undoBuffer = new Stack<>();
    private boolean isRebuildingSelection = false;

    private FormPanel palettePanel;
    private JPanel currentPaletteGroup;

    private JButton convertSelectedCellsButton;
    private JPopupMenu convertSelectedCellsMenu;
    private FormPanel.GroupHeaderPanel rowPaletteGroup;
    private FormPanel.GroupHeaderPanel columnPaletteGroup;
    private FormPanel.GroupHeaderPanel selectionPaletteGroup;
    private JToolBar toolBar = new JToolBar();

    /**
     * @param workbench  the workbench
     * @param tableModel the table
     */
    public TableEditor(JIPipeWorkbench workbench, ResultsTableData tableModel) {
        super(workbench);
        this.tableModel = tableModel;
        initialize();
        setTableModel(tableModel);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Add toolbar buttons

        toolBar.setFloatable(false);

        JButton openButton = new JButton("Open", UIUtils.getIconFromResources("actions/document-open-folder.png"));
        {
            JPopupMenu exportPopup = UIUtils.addPopupMenuToComponent(openButton);

            JMenuItem importFromCSV = new JMenuItem("from CSV table (*.csv)", UIUtils.getIconFromResources("data-types/results-table.png"));
            importFromCSV.addActionListener(e -> importFromCSV());
            exportPopup.add(importFromCSV);

            JMenuItem importFromImageJ = new JMenuItem("from ImageJ", UIUtils.getIconFromResources("apps/imagej.png"));
            importFromImageJ.addActionListener(e -> importFromImageJ());
            exportPopup.add(importFromImageJ);
        }
        toolBar.add(openButton);

        JButton exportButton = new JButton("Save", UIUtils.getIconFromResources("actions/save.png"));
        {
            JPopupMenu exportPopup = UIUtils.addPopupMenuToComponent(exportButton);

            JMenuItem exportAsCSV = new JMenuItem("as CSV table (*.csv)", UIUtils.getIconFromResources("data-types/results-table.png"));
            exportAsCSV.addActionListener(e -> exportTableAsCSV());
            exportPopup.add(exportAsCSV);
        }
        toolBar.add(exportButton);

        JButton cloneDataButton = new JButton("Clone", UIUtils.getIconFromResources("data-types/results-table.png"));
        cloneDataButton.addActionListener(e -> cloneDataToNewTab());
        toolBar.add(cloneDataButton);

        toolBar.addSeparator();

        JButton undoButton = new JButton("Undo", UIUtils.getIconFromResources("actions/undo.png"));
        undoButton.addActionListener(e -> undo());
        toolBar.add(undoButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton createPlotButton = new JButton("Create plot", UIUtils.getIconFromResources("actions/office-chart-line.png"));
        createPlotButton.addActionListener(e -> createNewPlot());
        toolBar.add(createPlotButton);

        add(toolBar, BorderLayout.NORTH);

        // Create palette
        palettePanel = new FormPanel(MarkdownDocument.fromPluginResource("documentation/table-analyzer.md", new HashMap<>()),
                FormPanel.WITH_SCROLLING);
        JXPanel contentPanel = palettePanel.getContentPanel();
        // This is needed for our flow layout
        contentPanel.setScrollableWidthHint(ScrollableSizeHint.HORIZONTAL_STRETCH);
        contentPanel.setScrollableHeightHint(ScrollableSizeHint.VERTICAL_STRETCH);

        rowPaletteGroup = addPaletteGroup("Rows", UIUtils.getIconFromResources("actions/stock_select-row.png"));
        addActionToPalette("Add",
                "Adds an empty row at the end of the table.",
                UIUtils.getIconFromResources("actions/edit-table-insert-row-below.png"),
                this::addRow);
        addSeparatorToPalette();
        addActionToPalette("Remove",
                "Remove selected rows",
                UIUtils.getIconFromResources("actions/edit-table-delete-row.png"),
                this::removeSelectedRows);

        columnPaletteGroup = addPaletteGroup("Columns", UIUtils.getIconFromResources("actions/stock_select-column.png"));
        addActionToPalette("Add numeric",
                "Adds an empty numeric column with a custom name to the table.",
                UIUtils.getIconFromResources("actions/edit-table-insert-column-right.png"),
                () -> addColumn(false));
        addActionToPalette("Add string",
                "Adds an empty string column with a custom name to the table.",
                UIUtils.getIconFromResources("actions/edit-table-insert-column-right.png"),
                () -> addColumn(true));
        addActionToPalette("Duplicate",
                "Copies the selected column into a new one.",
                UIUtils.getIconFromResources("actions/edit-copy.png"),
                this::copyColumn);
        addActionToPalette("Combine",
                "Creates a new column that contains the values of the selected columns assigned by the pattern colum0=row0, column1=row0, ... for each row.",
                UIUtils.getIconFromResources("actions/statistics.png"),
                this::addNewCombinedColumn);
        addSeparatorToPalette();
        addActionToPalette("Rename",
                "Renames the selected column",
                UIUtils.getIconFromResources("actions/tag.png"),
                this::renameColumn);
        addSeparatorToPalette();
        addActionToPalette("Remove",
                "Remove selected columns",
                UIUtils.getIconFromResources("actions/edit-table-delete-column.png"),
                this::removeSelectedColumns);
        addActionToPalette("To numeric",
                "Converts to numeric column. If a string value could not be converted, it is replaced by zero",
                UIUtils.getIconFromResources("actions/edit-select-number.png"),
                this::selectedColumnsToNumeric);
        addActionToPalette("To string",
                "Converts to string column.",
                UIUtils.getIconFromResources("actions/edit-select-text.png"),
                this::selectedColumnsToString);

        selectionPaletteGroup = addPaletteGroup("Selection", UIUtils.getIconFromResources("actions/edit-select-all.png"));
        addActionToPalette("Whole row",
                "Expands the selection to the whole row",
                UIUtils.getIconFromResources("actions/stock_select-row.png"),
                this::selectWholeRow);
        addActionToPalette("Whole column",
                "Expands the selection to the whole column",
                UIUtils.getIconFromResources("actions/stock_select-column.png"),
                this::selectWholeColumn);
        addSeparatorToPalette();
        addActionToPalette("Invert",
                "Inverts the selection",
                UIUtils.getIconFromResources("actions/edit-select-invert.png"),
                this::invertSelection);
        addActionToPalette("Select equivalent",
                "Select all rows that contain the selection of values",
                UIUtils.getIconFromResources("actions/filter.png"),
                this::selectEquivalent);

        addPaletteGroup("View", UIUtils.getIconFromResources("actions/find.png"));

        addActionToPalette("Autosize columns",
                "Resizes the selected columns, so they fit their contents.",
                UIUtils.getIconFromResources("actions/itmages-resize.png"),
                this::autoSizeColumns);

        addPaletteGroup("Data", UIUtils.getIconFromResources("data-types/results-table.png"));
        addActionToPalette("Import",
                "Merges another JIPipe table into the current one",
                UIUtils.getIconFromResources("data-types/results-table.png"),
                this::mergeTables);
        addActionToPalette("Integrate",
                "Collapses the table into a one-row table by applying an integration operation on each column..",
                UIUtils.getIconFromResources("actions/statistics.png"),
                this::integrateColumns);
        addSeparatorToPalette();
        convertSelectedCellsButton = addActionToPalette("Convert cells",
                "Converts the values in the selected cells",
                UIUtils.getIconFromResources("actions/formula.png"),
                () -> {
                });
        convertSelectedCellsMenu = UIUtils.addPopupMenuToComponent(convertSelectedCellsButton);
        addActionToPalette("To ImageJ",
                "Exports the table to ImageJ",
                UIUtils.getIconFromResources("apps/imagej.png"),
                this::exportToImageJ);
        palettePanel.addVerticalGlue();

        jxTable = new JXTable();
        jxTable.setModel(tableModel);
        jxTable.setColumnSelectionAllowed(true);
        jxTable.setRowSelectionAllowed(true);
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setDefaultRenderer(String.class, new Renderer(this));
        jxTable.setDefaultRenderer(Double.class, new Renderer(this));
        jxTable.packAll();

        JScrollPane scrollPane = new JScrollPane(jxTable);
        JSplitPane splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, palettePanel, AutoResizeSplitPane.RATIO_3_TO_1);
        add(splitPane, BorderLayout.CENTER);

        jxTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> updateConvertMenu());
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    private void exportToImageJ() {
        JIPipe.getImageJAdapters().getAdapterForJIPipeData(ResultsTableData.class).convertJIPipeToImageJ(
                tableModel,
                true,
                false,
                "" + tableModel
        );
    }

    private void importFromImageJ() {
        JIPipeOpenTableFromImageJDialogUI dialog = new JIPipeOpenTableFromImageJDialogUI(getWorkbench());
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.pack();
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void selectedColumnsToString() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            if (jxTable.getSelectedColumns() != null) {
                createUndoSnapshot();
                Set<String> selection = new HashSet<>();
                for (int viewIndex : jxTable.getSelectedColumns()) {
                    int modelIndex = jxTable.convertColumnIndexToModel(viewIndex);
                    selection.add(tableModel.getColumnName(modelIndex));
                }
                for (String column : selection) {
                    int index = tableModel.getColumnIndex(column);
                    tableModel.convertToStringColumn(index);
                }

                refreshTable();
            }
        }
    }

    private void selectedColumnsToNumeric() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            if (jxTable.getSelectedColumns() != null) {
                createUndoSnapshot();
                Set<String> selection = new HashSet<>();
                for (int viewIndex : jxTable.getSelectedColumns()) {
                    int modelIndex = jxTable.convertColumnIndexToModel(viewIndex);
                    selection.add(tableModel.getColumnName(modelIndex));
                }
                for (String column : selection) {
                    int index = tableModel.getColumnIndex(column);
                    tableModel.convertToNumericColumn(index);
                }

                refreshTable();
            }
        }
    }

    private void importFromCSV() {
        Path fileName = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Open CSV table (*.csv)", UIUtils.EXTENSION_FILTER_CSV);
        if (fileName != null) {
            ResultsTableData tableData = ResultsTableData.fromCSV(fileName);
            tableModel = tableData;
            jxTable.setModel(tableData);
            jxTable.packAll();
        }
    }

    private JButton addActionToPalette(String name, String description, Icon icon, Runnable action) {
        JButton button = new JButton(name, icon);
        button.setToolTipText(description);
        UIUtils.makeFlat(button);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.addActionListener(e -> action.run());
//        palettePanel.addWideToForm(button, new MarkdownDocument(description));
        currentPaletteGroup.add(button);
        return button;
    }

    private FormPanel.GroupHeaderPanel addPaletteGroup(String name, Icon icon) {
        FormPanel.GroupHeaderPanel groupHeaderPanel = palettePanel.addGroupHeader(name, icon);
        currentPaletteGroup = new JPanel();
        currentPaletteGroup.setLayout(new ModifiedFlowLayout(FlowLayout.LEFT));
        palettePanel.addWideToForm(currentPaletteGroup, null);
        return groupHeaderPanel;
    }

    private void addSeparatorToPalette() {
//        palettePanel.addWideToForm(Box.createVerticalStrut(8), null);
        currentPaletteGroup.add(Box.createVerticalStrut(8));
    }

    private void mergeTables() {
        JIPipeMergeTablesDialogUI dialog = new JIPipeMergeTablesDialogUI(this);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
        refreshTable();
    }

    private void createNewPlot() {
        PlotEditor plotBuilderUI = new PlotEditor(getWorkbench());
        plotBuilderUI.importData(tableModel, getWorkbench().getDocumentTabPane().findTabNameFor(this));
        getWorkbench().getDocumentTabPane().addTab("Plot",
                UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI,
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void integrateColumns() {
        JIPipeIntegrateTableColumnsDialogUI dialog = new JIPipeIntegrateTableColumnsDialogUI(tableModel);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
        if (dialog.getOutputTableModel() != null) {
            createUndoSnapshot();
            tableModel = dialog.getOutputTableModel();
            jxTable.setModel(tableModel);
            jxTable.packAll();
        }
    }

    private void undo() {
        if (!undoBuffer.isEmpty()) {
            tableModel = undoBuffer.pop();
            jxTable.setModel(tableModel);
            jxTable.packAll();
        }
    }

    private void selectEquivalent() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            if (jxTable.getSelectedRowCount() > 0 && jxTable.getSelectedColumnCount() > 0) {
                isRebuildingSelection = true;
                List<Object[]> possibleValues = new ArrayList<>();
                int[] columns = jxTable.getSelectedColumns().clone();
                for (int i = 0; i < columns.length; ++i) {
                    columns[i] = jxTable.convertColumnIndexToModel(columns[i]);
                }

                // Query all possible values
                for (int viewRow : jxTable.getSelectedRows()) {
                    Object[] tuple = new Object[columns.length];
                    for (int i = 0; i < columns.length; ++i) {
                        tuple[i] = tableModel.getValueAt(jxTable.convertRowIndexToModel(viewRow), columns[i]);
                    }
                    possibleValues.add(tuple);
                }

                jxTable.clearSelection();

                // Select all rows that match one of the possible values
                jxTable.addColumnSelectionInterval(0, tableModel.getColumnCount() - 1);
                for (int row = 0; row < tableModel.getRowCount(); ++row) {
                    boolean success = false;
                    for (Object[] possibleValue : possibleValues) {
                        boolean valueSuccess = true;
                        for (int i = 0; i < columns.length; ++i) {
                            if (!Objects.equals(tableModel.getValueAt(row, columns[i]), possibleValue[i])) {
                                valueSuccess = false;
                                break;
                            }
                        }
                        if (valueSuccess) {
                            success = true;
                            break;
                        }
                    }
                    if (success) {
                        jxTable.addRowSelectionInterval(jxTable.convertRowIndexToView(row), jxTable.convertRowIndexToView(row));
                    }
                }

                isRebuildingSelection = false;
                updateConvertMenu();
            }
        }
    }

    private void invertSelection() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            int[] cols = jxTable.getSelectedColumns().clone();
            int[] rows = jxTable.getSelectedRows().clone();
            isRebuildingSelection = true;
            jxTable.clearSelection();
            for (int column : cols) {
                jxTable.addColumnSelectionInterval(column, column);
            }
            for (int row = 0; row < jxTable.getRowCount(); ++row) {
                if (!Ints.contains(rows, row))
                    jxTable.addRowSelectionInterval(row, row);
            }
            isRebuildingSelection = false;
        }
        updateConvertMenu();
    }

    private void selectWholeColumn() {
        jxTable.addRowSelectionInterval(0, tableModel.getRowCount() - 1);
    }

    private void selectWholeRow() {
        jxTable.addColumnSelectionInterval(0, tableModel.getColumnCount() - 1);
    }

    private void cloneDataToNewTab() {
        getWorkbench().getDocumentTabPane().addTab("Table",
                UIUtils.getIconFromResources("data-types/results-table.png"),
                new TableEditor(getWorkbench(), new ResultsTableData(tableModel)),
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void updateConvertMenu() {
        if (isRebuildingSelection)
            return;
        convertSelectedCellsMenu.removeAll();
        for (JIPipeExpressionRegistry.ColumnOperationEntry entry :
                JIPipe.getTableOperations().getTableColumnOperationsOfType(ConvertingColumnOperation.class)
                        .values().stream().sorted(Comparator.comparing(JIPipeExpressionRegistry.ColumnOperationEntry::getName)).collect(Collectors.toList())) {
            JMenuItem item = new JMenuItem(entry.getName(), UIUtils.getIconFromResources("actions/configure.png"));
            item.setToolTipText(entry.getDescription());
            item.addActionListener(e -> {
                createUndoSnapshot();
                tableModel.applyOperation(getSelectedCells(), (ConvertingColumnOperation) entry.getOperation());
            });
            convertSelectedCellsMenu.add(item);
        }
        convertSelectedCellsButton.setEnabled(convertSelectedCellsMenu.getComponentCount() > 0);
    }

    /**
     * Gets the selected cells
     *
     * @return
     */
    private List<ResultsTableData.Index> getSelectedCells() {
        List<ResultsTableData.Index> result = new ArrayList<>();
        if (jxTable.getSelectedRows() != null && jxTable.getSelectedColumns() != null) {
            for (int row : jxTable.getSelectedRows()) {
                for (int column : jxTable.getSelectedColumns()) {
                    result.add(new ResultsTableData.Index(row, column));
                }
            }
        }
        return result;
    }

    /**
     * Auto-sizes all columns
     */
    public void autoSizeColumns() {
        jxTable.packAll();
    }

    private void addColumn(boolean stringColumn) {
        String name = UIUtils.getUniqueStringByDialog(this,
                "Please enter the name of the new column",
                "Column",
                tableModel::containsColumn);
        if (name != null && !name.isEmpty()) {
            createUndoSnapshot();
            if (tableModel.getRowCount() == 0)
                tableModel.addRow();
            tableModel.addColumn(name, stringColumn);
            refreshTable();
        }
    }

    private void copyColumn() {
        int sourceColumn = jxTable.getSelectedColumn();

        if (sourceColumn == -1)
            return;

        sourceColumn = jxTable.convertColumnIndexToModel(sourceColumn);
        String sourceColumnName = tableModel.getColumnName(sourceColumn);

        String newName = UIUtils.getUniqueStringByDialog(this, "Set the name of the new column:",
                sourceColumnName, tableModel::containsColumn);
        if (!StringUtils.isNullOrEmpty(newName) && !tableModel.containsColumn(newName)) {
            createUndoSnapshot();
            jxTable.setModel(new DefaultTableModel());
            tableModel.duplicateColumn(sourceColumn, newName);
            jxTable.setModel(tableModel);
            refreshTable();
        }
    }

    private void addNewCombinedColumn() {
        if (jxTable.getSelectedColumns() != null && jxTable.getSelectedColumns().length > 0) {
            int[] sourceColumns = jxTable.getSelectedColumns();
            for (int i = 0; i < sourceColumns.length; ++i) {
                sourceColumns[i] = jxTable.convertColumnIndexToModel(sourceColumns[i]);
            }

            StringBuilder generatedName = new StringBuilder();
            for (int i = 0; i < sourceColumns.length; ++i) {
                if (i > 0)
                    generatedName.append(", ");
                generatedName.append(tableModel.getColumnName(sourceColumns[i]));
            }

            String newName = UIUtils.getUniqueStringByDialog(this, "Set the name of the new column:",
                    generatedName.toString(), tableModel::containsColumn);
            if (!StringUtils.isNullOrEmpty(newName) && !tableModel.containsColumn(newName)) {
                try (BusyCursor busyCursor = new BusyCursor(this)) {
                    createUndoSnapshot();
                    tableModel.mergeColumns(Ints.asList(sourceColumns), newName, ", ", "=");
                    refreshTable();
                }
            }
        }
    }

    private void renameColumn() {
        if (jxTable.getSelectedColumn() != -1) {
            createUndoSnapshot();
            String oldName = tableModel.getColumnName(jxTable.convertColumnIndexToModel(jxTable.getSelectedColumn()));
            String name = UIUtils.getUniqueStringByDialog(this, "Please input a new name", oldName, tableModel::containsColumn);
            if (!StringUtils.isNullOrEmpty(name) && !Objects.equals(name, oldName)) {
                jxTable.setModel(new DefaultTableModel());
                tableModel.renameColumn(oldName, name);
                jxTable.setModel(tableModel);
                jxTable.packAll();
            }
        }
    }

    private void refreshTable() {
        jxTable.setModel(new DefaultTableModel());
        jxTable.setModel(tableModel);
        jxTable.packAll();
        updateSelectionStatistics();
    }

    private void addRow() {
        tableModel.addRow();
        refreshTable();
        updateSelectionStatistics();
    }

    private void removeSelectedColumns() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            if (jxTable.getSelectedColumns() != null) {
                createUndoSnapshot();
                Set<String> removedColumns = new HashSet<>();
                for (int viewIndex : jxTable.getSelectedColumns()) {
                    int modelIndex = jxTable.convertColumnIndexToModel(viewIndex);
                    removedColumns.add(tableModel.getColumnName(modelIndex));
                }
                jxTable.setModel(new DefaultTableModel());
                tableModel.removeColumns(removedColumns);
                jxTable.setModel(tableModel);
                jxTable.packAll();
            }
        }
    }

    private void removeSelectedRows() {
        try (BusyCursor busyCursor = new BusyCursor(this)) {
            if (jxTable.getSelectedRows() != null) {
                createUndoSnapshot();
                List<Integer> rows = new ArrayList<>();
                for (int i = 0; i < jxTable.getSelectedRows().length; ++i) {
                    rows.add(jxTable.convertRowIndexToModel(jxTable.getSelectedRows()[i]));
                }
                tableModel.removeRows(rows);
                refreshTable();
                jxTable.packAll();
            }
        }
    }

    private void updateSelectionStatistics() {
        if (rowPaletteGroup != null) {
            int count = tableModel != null ? tableModel.getRowCount() : 0;
            rowPaletteGroup.getTitleLabel().setText(count > 0 ? "Rows (" + count + ")" : "Rows");
        }
        if (columnPaletteGroup != null) {
            int count = tableModel != null ? tableModel.getColumnCount() : 0;
            columnPaletteGroup.getTitleLabel().setText(count > 0 ? "Columns (" + count + ")" : "Columns");
        }
    }

    /**
     * Creates a new 'Undo' point
     */
    public void createUndoSnapshot() {
        if (undoBuffer.size() >= MAX_UNDO)
            undoBuffer.remove(0);

        undoBuffer.push(new ResultsTableData(tableModel));
    }

    private void exportTableAsCSV() {
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export CSV table (*.csv)", UIUtils.EXTENSION_FILTER_CSV);
        if (selectedPath != null) {
            try (BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(selectedPath.toFile()))) {
                String[] rowBuffer = new String[tableModel.getColumnCount()];

                for (int column = 0; column < tableModel.getColumnCount(); ++column) {
                    rowBuffer[column] = tableModel.getColumnName(column);
                }

                writer.write(Joiner.on(',').join(rowBuffer).getBytes(Charsets.UTF_8));
                writer.write("\n".getBytes(Charsets.UTF_8));

                for (int row = 0; row < tableModel.getRowCount(); ++row) {
                    for (int column = 0; column < tableModel.getColumnCount(); ++column) {
                        if (tableModel.getValueAt(row, column) instanceof Boolean) {
                            rowBuffer[column] = (Boolean) tableModel.getValueAt(row, column) ? "TRUE" : "FALSE";
                        } else if (tableModel.getValueAt(row, column) instanceof Number) {
                            rowBuffer[column] = tableModel.getValueAt(row, column).toString();
                        } else {
                            String content = "" + tableModel.getValueAt(row, column);
                            content = content.replace("\"", "\"\"");
                            if (content.contains(",")) {
                                content = "\"" + content + "\"";
                            }
                            rowBuffer[column] = content;
                        }
                    }
                    writer.write(Joiner.on(',').join(rowBuffer).getBytes(Charsets.UTF_8));
                    writer.write("\n".getBytes(Charsets.UTF_8));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ResultsTableData getTableModel() {
        return tableModel;
    }

    public void setTableModel(ResultsTableData tableModel) {
        this.tableModel = tableModel;
        jxTable.setModel(new ResultsTableData());
        jxTable.setModel(tableModel);
        if (tableModel != null) {
            tableModel.addTableModelListener(e -> {
                if (e.getSource() == tableModel)
                    updateSelectionStatistics();
            });
        }
        updateSelectionStatistics();
        SwingUtilities.invokeLater(this::autoSizeColumns);
    }

    /**
     * Imports a table from CSV and creates a new {@link TableEditor} tab
     *
     * @param fileName    CSV file
     * @param workbenchUI workbench
     */
    public static ResultsTableData importTableFromCSV(Path fileName, JIPipeProjectWorkbench workbenchUI) {
        ResultsTableData tableData = ResultsTableData.fromCSV(fileName);
        // Create table analyzer
        workbenchUI.getDocumentTabPane().addTab(fileName.getFileName().toString(), UIUtils.getIconFromResources("data-types/results-table.png"),
                new TableEditor(workbenchUI, tableData), DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        return tableData;
    }

    /**
     * Shows table data in a new window
     *
     * @param workbench the workbench
     * @param tableData the data
     * @param title     the title
     * @return the table editor component
     */
    public static TableEditor openWindow(JIPipeWorkbench workbench, ResultsTableData tableData, String title) {
        JFrame window = new JFrame(title);
        window.getContentPane().setLayout(new BorderLayout());
        TableEditor editor = new TableEditor(workbench, tableData);
        window.getContentPane().add(editor, BorderLayout.CENTER);
        window.setSize(1024, 768);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
        return editor;
    }

    /**
     * Renders a table cell
     */
    private static class Renderer extends JLabel implements TableCellRenderer {

        private final TableEditor tableEditor;

        /**
         * Creates a new renderer
         *
         * @param tableEditor the parent component
         */
        public Renderer(TableEditor tableEditor) {
            this.tableEditor = tableEditor;
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (!tableEditor.tableModel.isNumericColumn(column)) {
                setText("<html><i><span style=\"color:gray;\">\"</span>" + value + "<span style=\"color:gray;\">\"</span></i></html>");
            } else {
                setText("" + value);
            }

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }

            return this;
        }
    }
}
