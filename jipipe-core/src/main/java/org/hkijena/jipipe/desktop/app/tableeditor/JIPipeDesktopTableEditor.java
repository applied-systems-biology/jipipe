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

package org.hkijena.jipipe.desktop.app.tableeditor;

import com.google.common.html.HtmlEscapers;
import com.google.common.primitives.Ints;
import org.apache.commons.lang3.math.NumberUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.ploteditor.JFreeChartPlotEditor;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeTableViewerUIApplicationSettings;
import org.hkijena.jipipe.plugins.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spreadsheet UI
 */
public class JIPipeDesktopTableEditor extends JIPipeDesktopWorkbenchPanel {
    private static final int MAX_UNDO = 10;
    private final JIPipeTableViewerUIApplicationSettings settings;
    private final Stack<ResultsTableData> undoBuffer = new Stack<>();
    private final JPanel contentPanel = new JPanel(new BorderLayout());
    private ResultsTableData tableModel;
    private JXTable jxTable;
    private boolean isRebuildingSelection = false;
    private JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();

    /**
     * @param workbench  the workbench
     * @param tableModel the table
     */
    public JIPipeDesktopTableEditor(JIPipeDesktopWorkbench workbench, ResultsTableData tableModel) {
        super(workbench);
        if (JIPipe.getInstance() != null) {
            settings = JIPipeTableViewerUIApplicationSettings.getInstance();
        } else {
            settings = new JIPipeTableViewerUIApplicationSettings();
        }
        this.tableModel = tableModel;
        initialize();
        setTableModel(tableModel);
    }

    /**
     * Imports a table from CSV and creates a new editor tab
     *
     * @param fileName    CSV file
     * @param workbenchUI workbench
     */
    public static ResultsTableData importTableFromCSV(Path fileName, JIPipeDesktopProjectWorkbench workbenchUI) {
        ResultsTableData tableData = ResultsTableData.fromCSV(fileName);
        // Create table analyzer
        workbenchUI.getDocumentTabPane().addTab(fileName.getFileName().toString(), UIUtils.getIconFromResources("data-types/results-table.png"),
                new JIPipeDesktopTableEditor(workbenchUI, tableData), JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton, true);
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
    public static JIPipeDesktopTableEditor openWindow(JIPipeDesktopWorkbench workbench, ResultsTableData tableData, String title) {
        JFrame window = new JFrame(title);
        window.getContentPane().setLayout(new BorderLayout());
        window.setIconImage(UIUtils.getJIPipeIcon128());
        JIPipeDesktopTableEditor editor = new JIPipeDesktopTableEditor(workbench, tableData);
        window.getContentPane().add(editor, BorderLayout.CENTER);
        window.getContentPane().add(editor.getRibbon(), BorderLayout.NORTH);
        window.pack();
        window.setSize(1024, 768);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
        SwingUtilities.invokeLater(editor::repaint);
        return editor;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        // Generate ribbon
        ribbon.setNumRows(2);
        rebuildRibbon();

        jxTable = new JXTable();
        jxTable.setModel(tableModel);
        jxTable.setColumnSelectionAllowed(true);
        jxTable.setRowSelectionAllowed(true);
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setDefaultRenderer(String.class, new Renderer(this));
        jxTable.setDefaultRenderer(Double.class, new Renderer(this));
        jxTable.setDefaultEditor(Double.class, new NumberEditor());
        jxTable.packAll();

        getContentPanel().add(jxTable.getTableHeader(), BorderLayout.NORTH);
        getContentPanel().add(new JScrollPane(jxTable), BorderLayout.CENTER);
    }

    public void rebuildRibbon() {
        initializeTableRibbonTask();
        initializeSelectRibbonTask();
        initializeColumnsRibbonTask();
        initializeRowsRibbonTask();
        initializeViewRibbonTask();
        ribbon.rebuildRibbon();
    }

    private void initializeRowsRibbonTask() {
        JIPipeDesktopRibbon.Task rowsTask = ribbon.addTask("Rows");
        JIPipeDesktopRibbon.Band addBand = rowsTask.addBand("Add/Delete");

        addBand.add(new JIPipeDesktopLargeButtonRibbonAction("Add row", "Adds a new row", UIUtils.getIcon32FromResources("actions/edit-table-insert-row-below.png"), this::addRow));
        addBand.add(new JIPipeDesktopLargeButtonRibbonAction("Delete selection", "Deletes the selected rows", UIUtils.getIcon32FromResources("actions/delete.png"), this::removeSelectedRows));
    }

    private void initializeSelectRibbonTask() {
        JIPipeDesktopRibbon.Task selectTask = ribbon.addTask("Select");
        JIPipeDesktopRibbon.Band generalBand = selectTask.addBand("General");
        JIPipeDesktopRibbon.Band rowsBand = selectTask.addBand("Rows");
        JIPipeDesktopRibbon.Band columnsBand = selectTask.addBand("Columns");

        generalBand.add(new JIPipeDesktopLargeButtonRibbonAction("Invert selection", "Inverts the selection", UIUtils.getIcon32FromResources("actions/edit-select-invert.png"), this::invertSelection));

        rowsBand.add(new JIPipeDesktopLargeButtonRibbonAction("Select whole row", "Expands the selection to the whole row", UIUtils.getIcon32FromResources("actions/stock_select-row.png"), this::selectWholeRow));
        rowsBand.add(new JIPipeDesktopLargeButtonRibbonAction("Select equivalent rows", "Select all rows that contain the selection of values", UIUtils.getIcon32FromResources("actions/view-filter.png"), this::selectEquivalent));

        columnsBand.add(new JIPipeDesktopLargeButtonRibbonAction("Select whole column", "Expands the selection to the whole column", UIUtils.getIcon32FromResources("actions/stock_select-column.png"), this::selectWholeColumn));
    }

    private void initializeViewRibbonTask() {
        JIPipeDesktopRibbon.Task viewTask = ribbon.addTask("View");
        JIPipeDesktopRibbon.Band columnBand = viewTask.addBand("Columns");
        columnBand.add(new JIPipeDesktopLargeButtonRibbonAction("Auto-size columns", "Resizes the selected columns, so they fit their contents.", UIUtils.getIcon32FromResources("actions/resizecol.png"), this::autoSizeColumns));
    }

    private void initializeColumnsRibbonTask() {
        JIPipeDesktopRibbon.Task columnsTask = ribbon.addTask("Columns");
        JIPipeDesktopRibbon.Band addBand = columnsTask.addBand("Add/Delete");
        JIPipeDesktopRibbon.Band modifyBand = columnsTask.addBand("Modify");

        addBand.add(new JIPipeDesktopLargeButtonRibbonAction("New string column", "Adds a new string column", UIUtils.getIcon32FromResources("actions/edit-table-insert-column-right.png"), () -> addColumn(true)));
        addBand.add(new JIPipeDesktopLargeButtonRibbonAction("New numeric column", "Adds a new numeric column", UIUtils.getIcon32FromResources("actions/edit-table-insert-column-right.png"), () -> addColumn(false)));
        addBand.add(new JIPipeDesktopSmallButtonRibbonAction("Duplicate", "Duplicates the selected columns", UIUtils.getIconFromResources("actions/edit-duplicate.png"), this::copyColumn));
        addBand.add(new JIPipeDesktopSmallButtonRibbonAction("Delete", "Deletes the selected columns", UIUtils.getIconFromResources("actions/delete.png"), this::removeSelectedColumns));

        modifyBand.add(new JIPipeDesktopLargeButtonRibbonAction("Rename", "Renames the selected column", UIUtils.getIcon32FromResources("actions/tag.png"), this::renameColumn));
//        modifyBand.add(new LargeButtonAction("Combine", "Creates a new column that contains the values of the selected columns assigned by the pattern colum0=row0, column1=row0, ... for each row.", UIUtils.getIcon32FromResources("actions/rabbitvcs-merge.png"), this::addNewCombinedColumn));
        modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("To string column", "Converts the column to a string column", UIUtils.getIcon16FromResources("actions/edit-select-text.png"), this::selectedColumnsToString));
        modifyBand.add(new JIPipeDesktopSmallButtonRibbonAction("To numeric column", "Converts the column to a numeric column", UIUtils.getIcon16FromResources("actions/edit-select-number.png"), this::selectedColumnsToNumeric));
    }

    private void initializeTableRibbonTask() {
        JIPipeDesktopRibbon.Task tableTask = ribbon.getOrCreateTask("General");
        JIPipeDesktopRibbon.Band generalBand = tableTask.addBand("Edit");
        JIPipeDesktopRibbon.Band fileBand = tableTask.addBand("File");
        JIPipeDesktopRibbon.Band importExportBand = tableTask.addBand("Import/Export");
        JIPipeDesktopRibbon.Band dataBand = tableTask.addBand("Data");

        generalBand.add(new JIPipeDesktopLargeButtonRibbonAction("Undo", "Reverts the last operation", UIUtils.getIcon32FromResources("actions/edit-undo.png"), this::undo));

        fileBand.add(new JIPipeDesktopLargeButtonRibbonAction("Open", "Opens a table from a file", UIUtils.getIcon32FromResources("actions/fileopen.png"), this::openTableFromFile));
        fileBand.add(new JIPipeDesktopLargeButtonRibbonAction("Save", "Saves the table to a file", UIUtils.getIcon32FromResources("actions/document-save.png"), this::exportTableToFile));

        importExportBand.add(new JIPipeDesktopLargeButtonRibbonAction("Clone", "Creates a copy of the table in a new window", UIUtils.getIcon32FromResources("actions/entry-clone.png"), this::cloneTableToNewWindow));
        importExportBand.add(new JIPipeDesktopSmallButtonRibbonAction("From ImageJ", "Imports a table from ImageJ", UIUtils.getIcon16FromResources("apps/imagej.png"), this::importTableFromImageJ));
        importExportBand.add(new JIPipeDesktopSmallButtonRibbonAction("To ImageJ", "Exports the table to ImageJ", UIUtils.getIcon16FromResources("apps/imagej.png"), this::exportTableToImageJ));

        dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("Summarize", "Collapses the table into a one-row table by applying a sum operation on each column", UIUtils.getIcon32FromResources("actions/statistics.png"), this::summarizeColumns));
        {
            JIPipeDesktopLargeButtonRibbonAction convertButton = new JIPipeDesktopLargeButtonRibbonAction("Apply function", "Applies a function to the selected table cells", UIUtils.getIcon32FromResources("actions/insert-math-expression.png"), () -> {
            });
            JPopupMenu convertMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToButton(convertButton.getButton(), convertMenu, () -> updateConvertMenu(convertButton.getButton(), convertMenu));
            dataBand.add(convertButton);
        }
        dataBand.add(new JIPipeDesktopLargeButtonRibbonAction("Plot", "Creates a plot from this table", UIUtils.getIcon32FromResources("actions/labplot-xy-plot-two-axes.png"), this::createNewPlot));
    }

    private void exportTableToImageJ() {
        JIPipe.getImageJAdapters().getDefaultExporterFor(ResultsTableData.class).exportData(tableModel,
                new ImageJExportParameters(true, false, false, "" + tableModel), new JIPipeProgressInfo());
    }

    private void importTableFromImageJ() {
        JIPipeDesktopOpenTableFromImageJDialogUI dialog = new JIPipeDesktopOpenTableFromImageJDialogUI(getDesktopWorkbench());
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

    private void openTableFromFile() {
        Path fileName = JIPipeDesktop.openFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Open table", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (fileName != null) {
            ResultsTableData tableData;
            if (UIUtils.EXTENSION_FILTER_XLSX.accept(fileName.toFile())) {
                tableData = ResultsTableData.fromXLSX(fileName).values().iterator().next();
            } else {
                tableData = ResultsTableData.fromCSV(fileName);
            }
            tableModel = tableData;
            jxTable.setModel(tableData);
            jxTable.packAll();
        }
    }

    private void mergeTablesFromDifferentWindows() {
        // TODO: Currently non-functional
        JIPipeDesktopJIPipeMergeTablesDialogUI dialog = new JIPipeDesktopJIPipeMergeTablesDialogUI(this);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
        refreshTable();
    }

    private void createNewPlot() {
        JFreeChartPlotEditor plotBuilderUI = JFreeChartPlotEditor.openWindow(getDesktopWorkbench(), "Plot");
        plotBuilderUI.importData(tableModel, getDesktopWorkbench().getDocumentTabPane().findTabNameFor(this));
    }

    private void summarizeColumns() {
        JIPipeDesktopSummarizeTableColumnsDialogUI dialog = new JIPipeDesktopSummarizeTableColumnsDialogUI(tableModel);
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
    }

    private void selectWholeColumn() {
        jxTable.addRowSelectionInterval(0, tableModel.getRowCount() - 1);
    }

    private void selectWholeRow() {
        jxTable.addColumnSelectionInterval(0, tableModel.getColumnCount() - 1);
    }

    private void cloneTableToNewWindow() {
        openWindow(getDesktopWorkbench(), new ResultsTableData(tableModel), "Cloned table");
    }

    private void updateConvertMenu(JButton convertSelectedCellsButton, JPopupMenu convertSelectedCellsMenu) {
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
     * @return the cell indices
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
    }

    private void addRow() {
        tableModel.addRow();
        refreshTable();
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

    /**
     * Creates a new 'Undo' point
     */
    public void createUndoSnapshot() {
        if (undoBuffer.size() >= MAX_UNDO)
            undoBuffer.remove(0);

        undoBuffer.push(new ResultsTableData(tableModel));
    }

    private void exportTableToFile() {
        Path selectedPath = JIPipeDesktop.saveFile(this, getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export table", HTMLText.EMPTY, UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (selectedPath != null) {
            if (UIUtils.EXTENSION_FILTER_XLSX.accept(selectedPath.toFile())) {
                tableModel.saveAsXLSX(selectedPath);
            } else {
                tableModel.saveAsCSV(selectedPath);
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
        SwingUtilities.invokeLater(this::autoSizeColumns);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    public JIPipeDesktopRibbon getRibbon() {
        return ribbon;
    }

    public void setRibbon(JIPipeDesktopRibbon ribbon) {
        this.ribbon = ribbon;
        rebuildRibbon();
    }

    /**
     * Renders a table cell
     */
    private static class Renderer extends JLabel implements TableCellRenderer {

        private final JIPipeDesktopTableEditor tableEditor;

        /**
         * Creates a new renderer
         *
         * @param tableEditor the parent component
         */
        public Renderer(JIPipeDesktopTableEditor tableEditor) {
            this.tableEditor = tableEditor;
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (!tableEditor.tableModel.isNumericColumn(column)) {
                setText("<html><i><span style=\"color:gray;\">\"</span>" + HtmlEscapers.htmlEscaper().escape(StringUtils.nullToEmpty(value)) + "<span style=\"color:gray;\">\"</span></i></html>");
            } else {
                setText(String.valueOf(value));
            }

            if (isSelected) {
                setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                setBackground(UIManager.getColor("List.background"));
            }

            return this;
        }
    }

    public static class NumberEditor extends DefaultCellEditor {

        private Object value;

        public NumberEditor() {
            super(new JTextField());
        }

        @Override
        public Object getCellEditorValue() {
            return value;
        }

        @Override
        public boolean stopCellEditing() {
            JTextField textField = (JTextField) getComponent();
            boolean success = true;

            try {
                String editingValue = StringUtils.nullToEmpty(super.getCellEditorValue());
                editingValue = editingValue.replace(',', '.').replace(" ", "");
                if (NumberUtils.isCreatable(editingValue)) {
                    value = NumberUtils.createDouble(editingValue);
                } else if (StringUtils.isNullOrEmpty(editingValue)) {
                    value = 0d;
                } else if (editingValue.toLowerCase(Locale.ROOT).startsWith("-inf")) {
                    value = Double.NEGATIVE_INFINITY;
                } else if (editingValue.toLowerCase(Locale.ROOT).startsWith("inf")) {
                    value = Double.POSITIVE_INFINITY;
                } else if (editingValue.equalsIgnoreCase("na") || editingValue.equalsIgnoreCase("nan")) {
                    value = Double.NaN;
                } else {
                    success = false;
                }
            } catch (NumberFormatException exception) {
                success = false;
            }

            if (success) {
                textField.setBorder(UIManager.getBorder("TextField.border"));
                return super.stopCellEditing();
            } else {
                textField.setBorder(new LineBorder(Color.red));
                return false;
            }
        }
    }
}
