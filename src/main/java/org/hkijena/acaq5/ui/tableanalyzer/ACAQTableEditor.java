/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.tableanalyzer;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;
import org.hkijena.acaq5.api.registries.ACAQTableRegistry;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.settings.FileChooserSettings;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.components.FormPanel;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.ModifiedFlowLayout;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotBuilderUI;
import org.hkijena.acaq5.utils.BusyCursor;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spreadsheet UI
 */
public class ACAQTableEditor extends ACAQProjectWorkbenchPanel {
    private static final int MAX_UNDO = 10;
    private ResultsTableData tableModel;
    private JXTable jxTable;
    private Stack<ResultsTableData> undoBuffer = new Stack<>();
    private boolean isRebuildingSelection = false;

    private FormPanel palettePanel;
    private JPanel currentPaletteGroup;

    private JButton convertSelectedCellsButton;
    private JPopupMenu convertSelectedCellsMenu;

    /**
     * @param workbench  the workbench
     * @param tableModel the table
     */
    public ACAQTableEditor(ACAQProjectWorkbench workbench, ResultsTableData tableModel) {
        super(workbench);
        this.tableModel = tableModel;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Add toolbar buttons
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openButton = new JButton("Open", UIUtils.getIconFromResources("open.png"));
        {
            JPopupMenu exportPopup = UIUtils.addPopupMenuToComponent(openButton);

            JMenuItem importFromCSV = new JMenuItem("from CSV table (*.csv)", UIUtils.getIconFromResources("filetype-csv.png"));
            importFromCSV.addActionListener(e -> importFromCSV());
            exportPopup.add(importFromCSV);
        }
        toolBar.add(openButton);

        JButton exportButton = new JButton("Save", UIUtils.getIconFromResources("save.png"));
        {
            JPopupMenu exportPopup = UIUtils.addPopupMenuToComponent(exportButton);

            JMenuItem exportAsCSV = new JMenuItem("as CSV table (*.csv)", UIUtils.getIconFromResources("filetype-csv.png"));
            exportAsCSV.addActionListener(e -> exportTableAsCSV());
            exportPopup.add(exportAsCSV);
        }
        toolBar.add(exportButton);

        JButton cloneDataButton = new JButton("Clone", UIUtils.getIconFromResources("table.png"));
        cloneDataButton.addActionListener(e -> cloneDataToNewTab());
        toolBar.add(cloneDataButton);

        toolBar.addSeparator();

        JButton undoButton = new JButton("Undo", UIUtils.getIconFromResources("undo.png"));
        undoButton.addActionListener(e -> undo());
        toolBar.add(undoButton);

        toolBar.add(Box.createHorizontalGlue());

        JButton createPlotButton = new JButton("Create plot", UIUtils.getIconFromResources("graph.png"));
        createPlotButton.addActionListener(e -> createNewPlot());
        toolBar.add(createPlotButton);

        add(toolBar, BorderLayout.NORTH);

        // Create palette
        palettePanel = new FormPanel(MarkdownDocument.fromPluginResource("documentation/table-analyzer.md"),
                FormPanel.WITH_SCROLLING);

        addPaletteGroup("Rows", UIUtils.getIconFromResources("select-row.png"));
        addActionToPalette("Add",
                "Adds an empty row at the end of the table.",
                UIUtils.getIconFromResources("add-row.png"),
                this::addRow);
        addActionToPalette("Import",
                "Adds rows from another ACAQ5 table that is currently open",
                UIUtils.getIconFromResources("table.png"),
                this::mergeRows);
        addSeparatorToPalette();
        addActionToPalette("Remove",
                "Remove selected rows",
                UIUtils.getIconFromResources("remove-row.png"),
                this::removeSelectedRows);

        addPaletteGroup("Columns", UIUtils.getIconFromResources("select-column.png"));
        addActionToPalette("Add numeric",
                "Adds an empty numeric column with a custom name to the table.",
                UIUtils.getIconFromResources("add-column.png"),
                () -> addColumn(false));
        addActionToPalette("Add string",
                "Adds an empty string column with a custom name to the table.",
                UIUtils.getIconFromResources("add-column.png"),
                () -> addColumn(true));
        addActionToPalette("Import",
                "Adds columns from another ACAQ5 table that is currently open.",
                UIUtils.getIconFromResources("table.png"),
                this::mergeColumns);
        addActionToPalette("Duplicate",
                "Copies the selected column into a new one.",
                UIUtils.getIconFromResources("copy.png"),
                this::copyColumn);
        addActionToPalette("Combine",
                "Creates a new column that contains the values of the selected columns assigned by the pattern colum0=row0, column1=row0, ... for each row.",
                UIUtils.getIconFromResources("statistics.png"),
                this::addNewCombinedColumn);
        addSeparatorToPalette();
        addActionToPalette("Rename",
                "Renames the selected column",
                UIUtils.getIconFromResources("label.png"),
                this::renameColumn);
        addSeparatorToPalette();
        addActionToPalette("Remove",
                "Remove selected columns",
                UIUtils.getIconFromResources("remove-column.png"),
                this::removeSelectedColumns);
        addActionToPalette("To numeric",
                "Converts to numeric column. If a string value could not be converted, it is replaced by zero",
                UIUtils.getIconFromResources("number.png"),
                this::selectedColumnsToNumeric);
        addActionToPalette("To string",
                "Converts to string column.",
                UIUtils.getIconFromResources("text2.png"),
                this::selectedColumnsToString);

        addPaletteGroup("Selection", UIUtils.getIconFromResources("select.png"));
        addActionToPalette("Whole row",
                "Expands the selection to the whole row",
                UIUtils.getIconFromResources("select-row.png"),
                this::selectWholeRow);
        addActionToPalette("Whole column",
                "Expands the selection to the whole column",
                UIUtils.getIconFromResources("select-column.png"),
                this::selectWholeColumn);
        addSeparatorToPalette();
        addActionToPalette("Invert",
                "Inverts the selection",
                UIUtils.getIconFromResources("invert.png"),
                this::invertSelection);
        addActionToPalette("Select equivalent",
                "Select all rows that contain the selection of values",
                UIUtils.getIconFromResources("filter.png"),
                this::selectEquivalent);

        addPaletteGroup("View", UIUtils.getIconFromResources("search.png"));

        addActionToPalette("Autosize columns",
                "Resizes the selected columns, so they fit their contents.",
                UIUtils.getIconFromResources("column-autosize.png"),
                this::autoSizeColumns);

        addPaletteGroup("Data", UIUtils.getIconFromResources("table.png"));
        addActionToPalette("Split",
                "Allows to split the data of a value column by a user-defined list of reference columns.",
                UIUtils.getIconFromResources("split.png"),
                this::splitColumns);
        addActionToPalette("Integrate",
                "Collapses the table into a one-row table by applying an integration operation on each column..",
                UIUtils.getIconFromResources("statistics.png"),
                this::collapseColumns);
        addSeparatorToPalette();
        convertSelectedCellsButton = addActionToPalette("Convert cells",
                "Converts the values in the selected cells",
                UIUtils.getIconFromResources("inplace-function.png"),
                () -> {
                });
        convertSelectedCellsMenu = UIUtils.addPopupMenuToComponent(convertSelectedCellsButton);
        palettePanel.addVerticalGlue();

        jxTable = new JXTable();
        jxTable.setModel(tableModel);
        jxTable.setColumnSelectionAllowed(true);
        jxTable.setRowSelectionAllowed(true);
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setDefaultRenderer(String.class, new Renderer(this));
        jxTable.setDefaultRenderer(Double.class, new Renderer(this));
        jxTable.packAll();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(jxTable), palettePanel);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(0.66);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                splitPane.setDividerLocation(0.66);
            }
        });

        add(splitPane, BorderLayout.CENTER);

        jxTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> updateConvertMenu());
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
        Path fileName = FileChooserSettings.openFile(this, FileChooserSettings.KEY_PROJECT, "Open *.csv");
        if(fileName != null) {
            try {
                ResultsTableData tableData = ResultsTableData.fromCSV(fileName);
                tableModel = tableData;
                jxTable.setModel(tableData);
                jxTable.packAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

    private void addPaletteGroup(String name, Icon icon) {
        palettePanel.addGroupHeader(name, icon);
        currentPaletteGroup = new JPanel();
        currentPaletteGroup.setLayout(new ModifiedFlowLayout(FlowLayout.LEFT));
        palettePanel.addWideToForm(currentPaletteGroup, null);
    }

    private void addSeparatorToPalette() {
//        palettePanel.addWideToForm(Box.createVerticalStrut(8), null);
        currentPaletteGroup.add(Box.createVerticalStrut(8));
    }

    private void splitColumns() {
        // TODO: Implement
//        ACAQSplitColumnDialogUI dialog = new ACAQSplitColumnDialogUI(tableModel);
//        dialog.pack();
//        dialog.setSize(800, 600);
//        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
//        dialog.setModal(true);
//        dialog.setVisible(true);
//        if (dialog.getResultTableModel() != null) {
//            createUndoSnapshot();
//            tableModel = dialog.getResultTableModel();
//            jxTable.setModel(tableModel);
//            jxTable.packAll();
//        }
    }

    private void mergeColumns() {
        ACAQMergeTableColumnsDialogUI dialog = new ACAQMergeTableColumnsDialogUI(this);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void mergeRows() {
        ACAQMergeTableRowsDialogUI dialog = new ACAQMergeTableRowsDialogUI(this);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
    }

    private void createNewPlot() {
        ACAQPlotBuilderUI plotBuilderUI = new ACAQPlotBuilderUI(getProjectWorkbench());
        plotBuilderUI.importData(tableModel, getProjectWorkbench().documentTabPane.findTabNameFor(this));
        getProjectWorkbench().getDocumentTabPane().addTab("Plot",
                UIUtils.getIconFromResources("data-types/data-type-plot.png"),
                plotBuilderUI,
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getProjectWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void collapseColumns() {
        // TODO: Implement
//        ACAQCollapseTableColumnsDialogUI dialog = new ACAQCollapseTableColumnsDialogUI(tableModel);
//        dialog.pack();
//        dialog.setSize(800, 600);
//        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
//        dialog.setModal(true);
//        dialog.setVisible(true);
//        if (dialog.getResultTableModel() != null) {
//            createUndoSnapshot();
//            tableModel = dialog.getResultTableModel();
//            jxTable.setModel(tableModel);
//            jxTable.packAll();
//        }
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
        getProjectWorkbench().getDocumentTabPane().addTab("Table",
                UIUtils.getIconFromResources("table.png"),
                new ACAQTableEditor(getProjectWorkbench(), new ResultsTableData(tableModel)),
                DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        getProjectWorkbench().getDocumentTabPane().switchToLastTab();
    }

    private void updateConvertMenu() {
        if (isRebuildingSelection)
            return;
        convertSelectedCellsMenu.removeAll();
        for (ACAQTableRegistry.ColumnOperationEntry entry :
                ACAQTableRegistry.getInstance().getOperationsOfType(ConvertingColumnOperation.class)
                        .values().stream().sorted(Comparator.comparing(ACAQTableRegistry.ColumnOperationEntry::getName)).collect(Collectors.toList())) {
            JMenuItem item = new JMenuItem(entry.getName(), UIUtils.getIconFromResources("cog.png"));
            item.setToolTipText(entry.getDescription());
            item.addActionListener(e -> {
                createUndoSnapshot();
                tableModel.applyOperation(getSelectedCells(), (ConvertingColumnOperation)entry.getOperation());
            });
            convertSelectedCellsMenu.add(item);
        }

        // TODO: Implement
//        final int cellCount = jxTable.getSelectedColumnCount() * jxTable.getSelectedRowCount();
//
//        List<ACAQTableAnalyzerUIOperationRegistry.VectorOperationEntry> entries =
//                new ArrayList<>(ACAQDefaultRegistry.getInstance().getTableAnalyzerUIOperationRegistry().getVectorOperationEntries());
//        entries.sort(Comparator.comparing(ACAQTableAnalyzerUIOperationRegistry.VectorOperationEntry::getName));
//
//        for (ACAQTableAnalyzerUIOperationRegistry.VectorOperationEntry vectorOperationEntry : entries) {
//            ACAQTableVectorOperation operation = vectorOperationEntry.instantiateOperation();
//            if (operation.inputMatches(cellCount) && operation.getOutputCount(cellCount) == cellCount) {
//                JMenuItem item = new JMenuItem(vectorOperationEntry.getName(), vectorOperationEntry.getIcon());
//                item.setToolTipText(vectorOperationEntry.getDescription());
//                item.addActionListener(e -> {
//
//                    createUndoSnapshot();
//
//                    List<Index> selectedCells = getSelectedCells();
//                    assert cellCount == selectedCells.size();
//
//                    Object[] buffer = new Object[cellCount];
//                    for (int i = 0; i < cellCount; ++i) {
//                        buffer[i] = tableModel.getValueAt(selectedCells.get(i).getRow(), selectedCells.get(i).getColumn());
//                    }
//
//                    buffer = operation.process(buffer);
//                    Vector tableData = tableModel.getDataVector();
//
//                    for (int i = 0; i < cellCount; ++i) {
//                        ((Vector) tableData.get(selectedCells.get(i).getRow())).set(selectedCells.get(i).getColumn(), buffer[i]);
//                    }
//
//                    tableModel.setDataVector(tableData, TableUtils.getColumnIdentifiers(tableModel));
//                    jxTable.packAll();
//                });
//                convertSelectedCellsMenu.add(item);
//            }
//        }

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
        String name = JOptionPane.showInputDialog(this,
                "Please provide a name for the new column", "Column " + (tableModel.getColumnCount() + 1));
        if (name != null && !name.isEmpty()) {
            createUndoSnapshot();
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
            tableModel.duplicateColumn(sourceColumn, newName);
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
                tableModel.renameColumn(oldName, name);
                refreshTable();
            }
        }
    }

    private void refreshTable() {
        jxTable.setModel(new DefaultTableModel());
        jxTable.setModel(tableModel);
        jxTable.packAll();
    }

    private void addRow() {
        tableModel.addRow();
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
                tableModel.removeColumns(removedColumns);
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
                rows.sort(Comparator.naturalOrder());

                tableModel = tableModel.getRows(rows);
                jxTable.packAll();
            }
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
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.KEY_PROJECT, "Export CSV table (*.csv)");
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

    /**
     * Imports a table from CSV and creates a new {@link ACAQTableEditor} tab
     *
     * @param fileName    CSV file
     * @param workbenchUI workbench
     */
    public static void importTableFromCSV(Path fileName, ACAQProjectWorkbench workbenchUI) {
        try {
            ResultsTableData tableData = ResultsTableData.fromCSV(fileName);
            // Create table analyzer
            workbenchUI.getDocumentTabPane().addTab(fileName.getFileName().toString(), UIUtils.getIconFromResources("table.png"),
                    new ACAQTableEditor(workbenchUI, tableData), DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Renders a table cell
     */
    private static class Renderer extends JLabel implements TableCellRenderer {

        private final ACAQTableEditor tableEditor;

        /**
         * Creates a new renderer
         * @param tableEditor the parent component
         */
        public Renderer(ACAQTableEditor tableEditor) {
            this.tableEditor = tableEditor;
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if(!tableEditor.tableModel.isNumeric(column)) {
                setText("<html><i><span style=\"color:gray;\">\"</span>" + value + "<span style=\"color:gray;\">\"</span></i></html>");
            }
            else {
                setText("" + value);
            }

            if (isSelected) {
                setBackground(new Color(184, 207, 229));
            } else {
                setBackground(new Color(255, 255, 255));
            }

            return this;
        }
    }
}
