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

import com.google.common.primitives.Ints;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.registries.JIPipeExpressionRegistry;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.settings.TableViewerUISettings;
import org.hkijena.jipipe.extensions.tables.ConvertingColumnOperation;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FlexContentWorkbenchPanel;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.plotbuilder.PlotEditor;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spreadsheet UI
 */
public class TableEditor extends FlexContentWorkbenchPanel {
    private static final int MAX_UNDO = 10;
    private final TableViewerUISettings settings;
    private final Stack<ResultsTableData> undoBuffer = new Stack<>();
    private ResultsTableData tableModel;
    private JXTable jxTable;
    private boolean isRebuildingSelection = false;

    /**
     * @param workbench  the workbench
     * @param tableModel the table
     */
    public TableEditor(JIPipeWorkbench workbench, ResultsTableData tableModel) {
        super(workbench, WITH_RIBBON);
        if (JIPipe.getInstance() != null) {
            settings = TableViewerUISettings.getInstance();
        } else {
            settings = new TableViewerUISettings();
        }
        this.tableModel = tableModel;
        initialize();
        setTableModel(tableModel);
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
        window.setIconImage(UIUtils.getIcon128FromResources("jipipe.png").getImage());
        TableEditor editor = new TableEditor(workbench, tableData);
        window.getContentPane().add(editor, BorderLayout.CENTER);
        window.setSize(1024, 768);
        window.setLocationRelativeTo(workbench.getWindow());
        window.setVisible(true);
        SwingUtilities.invokeLater(editor::repaint);
        return editor;
    }

    public static void main(String[] args) {
        JIPipeUITheme.ModernLight.install();
        JFrame frame = new JFrame();
        JIPipeWorkbench workbench = new JIPipeDummyWorkbench();
        TableEditor editor = new TableEditor(workbench, ResultsTableData.fromCSV(Paths.get("/data/JIPipe/metadata.csv")));
        frame.setContentPane(editor);
        frame.setSize(1024, 768);
        frame.setVisible(true);
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Generate ribbon
        initializeRibbon();

        jxTable = new JXTable();
        jxTable.setModel(tableModel);
        jxTable.setColumnSelectionAllowed(true);
        jxTable.setRowSelectionAllowed(true);
        jxTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jxTable.setDefaultRenderer(String.class, new Renderer(this));
        jxTable.setDefaultRenderer(Double.class, new Renderer(this));
        jxTable.packAll();

        getContentPanel().add(jxTable.getTableHeader(), BorderLayout.NORTH);
        getContentPanel().add(new JScrollPane(jxTable), BorderLayout.CENTER);

        rebuildLayout();
    }

    private void initializeRibbon() {
        Ribbon ribbon = getRibbon();
        ribbon.setNumRows(2);
        initializeTableRibbonTask(ribbon);
        initializeSelectRibbonTask(ribbon);
        initializeColumnsRibbonTask(ribbon);
        initializeRowsRibbonTask(ribbon);
        initializeViewRibbonTask(ribbon);
        ribbon.rebuildRibbon();
    }

    private void initializeRowsRibbonTask(Ribbon ribbon) {
        Ribbon.Task rowsTask = ribbon.addTask("Rows");
        Ribbon.Band addBand = rowsTask.addBand("Add/Delete");

        addBand.add(new LargeButtonAction("Add row", "Adds a new row", UIUtils.getIcon32FromResources("actions/edit-table-insert-row-below.png"), this::addRow));
        addBand.add(new LargeButtonAction("Delete selection", "Deletes the selected rows", UIUtils.getIcon32FromResources("actions/delete.png"), this::removeSelectedRows));
    }

    private void initializeSelectRibbonTask(Ribbon ribbon) {
        Ribbon.Task selectTask = ribbon.addTask("Select");
        Ribbon.Band generalBand = selectTask.addBand("General");
        Ribbon.Band rowsBand = selectTask.addBand("Rows");
        Ribbon.Band columnsBand = selectTask.addBand("Columns");

        generalBand.add(new LargeButtonAction("Invert selection", "Inverts the selection", UIUtils.getIcon32FromResources("actions/edit-select-invert.png"), this::invertSelection));

        rowsBand.add(new LargeButtonAction("Select whole row", "Expands the selection to the whole row", UIUtils.getIcon32FromResources("actions/stock_select-row.png"), this::selectWholeRow));
        rowsBand.add(new LargeButtonAction("Select equivalent rows", "Select all rows that contain the selection of values", UIUtils.getIcon32FromResources("actions/view-filter.png"), this::selectEquivalent));

        columnsBand.add(new LargeButtonAction("Select whole column", "Expands the selection to the whole column", UIUtils.getIcon32FromResources("actions/stock_select-column.png"), this::selectWholeColumn));
    }

    private void initializeViewRibbonTask(Ribbon ribbon) {
        Ribbon.Task viewTask = ribbon.addTask("View");
        Ribbon.Band columnBand = viewTask.addBand("Columns");
        columnBand.add(new LargeButtonAction("Auto-size columns", "Resizes the selected columns, so they fit their contents.", UIUtils.getIcon32FromResources("actions/resizecol.png"), this::autoSizeColumns));
    }

    private void initializeColumnsRibbonTask(Ribbon ribbon) {
        Ribbon.Task columnsTask = ribbon.addTask("Columns");
        Ribbon.Band addBand = columnsTask.addBand("Add/Delete");
        Ribbon.Band modifyBand = columnsTask.addBand("Modify");

        addBand.add(new LargeButtonAction("New string column", "Adds a new string column", UIUtils.getIcon32FromResources("actions/edit-table-insert-column-right.png"), () -> addColumn(true)));
        addBand.add(new LargeButtonAction("New numeric column", "Adds a new numeric column", UIUtils.getIcon32FromResources("actions/edit-table-insert-column-right.png"), () -> addColumn(false)));
        addBand.add(new SmallButtonAction("Duplicate", "Duplicates the selected columns", UIUtils.getIconFromResources("actions/edit-duplicate.png"), this::copyColumn));
        addBand.add(new SmallButtonAction("Delete", "Deletes the selected columns", UIUtils.getIconFromResources("actions/delete.png"), this::removeSelectedColumns));

        modifyBand.add(new LargeButtonAction("Rename", "Renames the selected column", UIUtils.getIcon32FromResources("actions/tag.png"), this::renameColumn));
//        modifyBand.add(new LargeButtonAction("Combine", "Creates a new column that contains the values of the selected columns assigned by the pattern colum0=row0, column1=row0, ... for each row.", UIUtils.getIcon32FromResources("actions/rabbitvcs-merge.png"), this::addNewCombinedColumn));
        modifyBand.add(new SmallButtonAction("To string column", "Converts the column to a string column", UIUtils.getIcon16FromResources("actions/edit-select-text.png"), this::selectedColumnsToString));
        modifyBand.add(new SmallButtonAction("To numeric column", "Converts the column to a numeric column", UIUtils.getIcon16FromResources("actions/edit-select-number.png"), this::selectedColumnsToNumeric));
    }

    private void initializeTableRibbonTask(Ribbon ribbon) {
        Ribbon.Task tableTask = ribbon.addTask("Table");
        Ribbon.Band generalBand = tableTask.addBand("General");
        Ribbon.Band fileBand = tableTask.addBand("File");
        Ribbon.Band importExportBand = tableTask.addBand("Import/Export");
        Ribbon.Band dataBand = tableTask.addBand("Data");

        generalBand.add(new LargeButtonAction("Undo", "Reverts the last operation", UIUtils.getIcon32FromResources("actions/edit-undo.png"), this::undo));

        fileBand.add(new LargeButtonAction("Open", "Opens a table from a file", UIUtils.getIcon32FromResources("actions/fileopen.png"), this::openTableFromFile));
        fileBand.add(new LargeButtonAction("Save", "Saves the table to a file", UIUtils.getIcon32FromResources("actions/document-save.png"), this::exportTableToFile));

        importExportBand.add(new LargeButtonAction("Clone", "Creates a copy of the table in a new window", UIUtils.getIcon32FromResources("actions/entry-clone.png"), this::cloneTableToNewWindow));
        importExportBand.add(new SmallButtonAction("From ImageJ", "Imports a table from ImageJ", UIUtils.getIcon16FromResources("apps/imagej.png"), this::importTableFromImageJ));
        importExportBand.add(new SmallButtonAction("To ImageJ", "Exports the table to ImageJ", UIUtils.getIcon16FromResources("apps/imagej.png"), this::exportTableToImageJ));

        dataBand.add(new LargeButtonAction("Integrate", "Collapses the table into a one-row table by applying an integration operation on each column", UIUtils.getIcon32FromResources("actions/statistics.png"), this::integrateColumns));
        {
            LargeButtonAction convertButton = new LargeButtonAction("Apply function", "Applies a function to the selected table cells", UIUtils.getIcon32FromResources("actions/insert-math-expression.png"), () -> {
            });
            JPopupMenu convertMenu = new JPopupMenu();
            UIUtils.addReloadablePopupMenuToComponent(convertButton.getButton(), convertMenu, () -> updateConvertMenu(convertButton.getButton(), convertMenu));
            dataBand.add(convertButton);
        }
        dataBand.add(new LargeButtonAction("Plot", "Creates a plot from this table", UIUtils.getIcon32FromResources("actions/labplot-xy-plot-two-axes.png"), this::createNewPlot));
    }

    private void exportTableToImageJ() {
        JIPipe.getImageJAdapters().getDefaultExporterFor(ResultsTableData.class).exportData(tableModel,
                new ImageJExportParameters(true, false, false, "" + tableModel), new JIPipeProgressInfo());
    }

    private void importTableFromImageJ() {
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

    private void openTableFromFile() {
        Path fileName = FileChooserSettings.openFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Open table", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
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
        JIPipeMergeTablesDialogUI dialog = new JIPipeMergeTablesDialogUI(this);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setModal(true);
        dialog.setVisible(true);
        refreshTable();
    }

    private void createNewPlot() {
        PlotEditor plotBuilderUI = PlotEditor.openWindow(getWorkbench(), "Plot");
        plotBuilderUI.importData(tableModel, getWorkbench().getDocumentTabPane().findTabNameFor(this));
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
        openWindow(getWorkbench(), new ResultsTableData(tableModel), "Cloned table");
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
        Path selectedPath = FileChooserSettings.saveFile(this, FileChooserSettings.LastDirectoryKey.Projects, "Export table", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
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
