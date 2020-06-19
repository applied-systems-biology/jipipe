package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import ij.macro.Variable;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumnReference;
import org.hkijena.acaq5.extensions.tables.operations.ConvertingColumnOperation;
import org.hkijena.acaq5.extensions.tables.operations.IntegratingColumnOperation;
import org.hkijena.acaq5.ui.ACAQProjectWorkbench;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableEditor;
import org.hkijena.acaq5.utils.PathUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data containing a {@link ResultsTable}
 */
@ACAQDocumentation(name = "Results table")
public class ResultsTableData implements ACAQData, TableModel {

    private ResultsTable table;
    private List<TableModelListener> listeners = new ArrayList<>();

    /**
     * Creates a new instance
     */
    public ResultsTableData() {
        this.table = new ResultsTable();
    }

    /**
     * Creates a {@link ResultsTableData} from a map of column name to column data
     *
     * @param columns key is column heading
     */
    public ResultsTableData(Map<String, TableColumn> columns) {
        importFromColumns(columns);
    }

    /**
     * Creates a {@link ResultsTableData} from a list of columns.
     * Column headings are extracted from the column labels
     *
     * @param columns the columns
     */
    public ResultsTableData(Collection<TableColumn> columns) {
        Map<String, TableColumn> map = new HashMap<>();
        for (TableColumn column : columns) {
            map.put(column.getLabel(), column);
        }
        importFromColumns(map);
    }

    /**
     * Loads a results table from a folder containing CSV file
     *
     * @param storageFilePath storage folder
     * @throws IOException triggered by {@link ResultsTable}
     */
    public ResultsTableData(Path storageFilePath) throws IOException {
        table = ResultsTable.open(PathUtils.findFileByExtensionIn(storageFilePath, ".csv").toString());
    }

    /**
     * Wraps a results table
     *
     * @param table wrapped table
     */
    public ResultsTableData(ResultsTable table) {
        this.table = table;
        cleanupTable();
    }


    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ResultsTableData(ResultsTableData other) {
        this.table = (ResultsTable) other.table.clone();
    }

    /**
     * Loads a table from CSV
     *
     * @param file the file
     * @return the table
     * @throws IOException thrown by {@link ResultsTable}
     */
    public static ResultsTableData fromCSV(Path file) throws IOException {
        return new ResultsTableData(ResultsTable.open(file.toString()));
    }

    private void importFromColumns(Map<String, TableColumn> columns) {
        this.table = new ResultsTable();

        // Collect map column name -> index
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (String column : columns.keySet()) {
            columnIndexMap.put(column, table.getFreeColumn(column));
        }

        // Collect the number of rows
        int rows = 0;
        for (TableColumn column : columns.values()) {
            rows = Math.max(rows, column.getRows());
        }

        // Create table
        for (int row = 0; row < rows; row++) {
            table.incrementCounter();
            for (Map.Entry<String, TableColumn> entry : columns.entrySet()) {
                int col = columnIndexMap.get(entry.getKey());
                if (entry.getValue().isNumeric()) {
                    table.setValue(col, row, entry.getValue().getRowAsDouble(row));
                } else {
                    table.setValue(col, row, entry.getValue().getRowAsString(row));
                }
            }
        }
    }

    /**
     * Applies an operation to the selected cells
     *
     * @param selectedCells the selected cells
     * @param operation     the operation
     */
    public void applyOperation(List<Index> selectedCells, ConvertingColumnOperation operation) {
        Map<Integer, List<Index>> byColumn = selectedCells.stream().collect(Collectors.groupingBy(Index::getColumn));
        for (Map.Entry<Integer, List<Index>> entry : byColumn.entrySet()) {
            int col = entry.getKey();
            TableColumn buffer;
            boolean numeric = isNumeric(col);
            if (numeric) {
                double[] data = new double[entry.getValue().size()];
                for (int i = 0; i < entry.getValue().size(); i++) {
                    int row = entry.getValue().get(i).row;
                    data[i] = getValueAsDouble(row, col);
                }
                buffer = new DoubleArrayTableColumn(data, "buffer");
            } else {
                String[] data = new String[entry.getValue().size()];
                for (int i = 0; i < entry.getValue().size(); i++) {
                    int row = entry.getValue().get(i).row;
                    data[i] = getValueAsString(row, col);
                }
                buffer = new StringArrayTableColumn(data, "buffer");
            }

            TableColumn result = operation.apply(buffer);
            for (int i = 0; i < entry.getValue().size(); i++) {
                int row = entry.getValue().get(i).row;
                Object value = numeric ? result.getRowAsDouble(i) : result.getRowAsString(i);
                setValueAt(value, row, col);
            }
        }
    }

    /**
     * Returns a column that contains all values of the selected columns
     *
     * @param columns   the columns to merge
     * @param separator the separator character
     * @param equals    the equals character
     * @return a column that contains all values of the selected columns
     */
    public TableColumn getMergedColumn(Set<String> columns, String separator, String equals) {
        String[] values = new String[getRowCount()];
        List<String> sortedColumns = columns.stream().sorted().collect(Collectors.toList());
        for (int row = 0; row < getRowCount(); row++) {
            int finalRow = row;
            values[row] = sortedColumns.stream().map(col -> col + equals + getValueAsString(finalRow, col)).collect(Collectors.joining(separator));
        }
        return new StringArrayTableColumn(values, String.join(separator, columns));
    }

    /**
     * Returns the row indices grouped by the values of the provided columns
     *
     * @param columns the columns to group by
     * @return a map from column name to column value to the rows that share the same values
     */
    public Map<Map<String, Object>, List<Integer>> getEquivalentRows(Set<String> columns) {
        Map<Map<String, Object>, List<Integer>> result = new HashMap<>();
        Set<Integer> columnIndices = columns.stream().map(this::getColumnIndex).collect(Collectors.toSet());
        for (int row = 0; row < getRowCount(); row++) {
            Map<String, Object> columnValues = new HashMap<>();
            for (Integer columnIndex : columnIndices) {
                String columnName = getColumnName(columnIndex);
                columnValues.put(columnName, getValueAt(row, columnIndex));
            }
            List<Integer> rowList = result.getOrDefault(columnValues, null);
            if (rowList == null) {
                rowList = new ArrayList<>();
                result.put(columnValues, rowList);
            }
            rowList.add(row);
        }
        return result;
    }

    /**
     * Generates a new table that contains statistics.
     * Optionally, statistics can be created for each category (based on the category column)
     *
     * @param operations the operations
     * @param categories columns considered as categorical. They are added to the output without changes.
     * @return integrated table
     */
    public ResultsTableData getStatistics(List<IntegratingColumnOperationEntry> operations, Set<String> categories) {
        ResultsTableData result = new ResultsTableData();

        if (categories == null || categories.isEmpty()) {
            result.addRow();
            for (IntegratingColumnOperationEntry operation : operations) {
                TableColumn inputColumn = getColumnReference(getColumnIndex(operation.getSourceColumnName()));
                TableColumn outputColumn = operation.getOperation().apply(inputColumn);
                if (outputColumn.isNumeric()) {
                    result.setValueAt(outputColumn.getDataAsDouble(0), 0, result.addColumn(operation.getTargetColumnName(), false));
                } else {
                    result.setValueAt(outputColumn.getRowAsString(0), 0, result.addColumn(operation.getTargetColumnName(), true));
                }
            }
        } else {
            Map<String, Integer> targetColumnIndices = new HashMap<>();
            // Create category columns first
            for (String category : categories) {
                targetColumnIndices.put(category, result.addColumn(category, isNumeric(getColumnIndex(category))));
            }
            Map<Map<String, Object>, List<Integer>> equivalentRows = getEquivalentRows(categories);
            int row = 0;
            for (Map.Entry<Map<String, Object>, List<Integer>> entry : equivalentRows.entrySet()) {
                result.addRow();
                // Write category columns
                for (Map.Entry<String, Object> categoryEntry : entry.getKey().entrySet()) {
                    result.setValueAt(categoryEntry.getValue(), row, result.getColumnIndex(categoryEntry.getKey()));
                }

                // Apply statistics
                for (IntegratingColumnOperationEntry operation : operations) {
                    TableColumn inputColumn = TableColumn.getSlice(getColumnReference(getColumnIndex(operation.getSourceColumnName())), entry.getValue());
                    TableColumn outputColumn = operation.getOperation().apply(inputColumn);
                    int col = targetColumnIndices.getOrDefault(operation.getTargetColumnName(), -1);
                    if (col == -1) {
                        col = result.addColumn(operation.getTargetColumnName(), !outputColumn.isNumeric());
                    }
                    if (result.isNumeric(col)) {
                        result.setValueAt(outputColumn.getRowAsDouble(0), row, col);
                    } else {
                        result.setValueAt(outputColumn.getRowAsString(0), row, col);
                    }
                }

                ++row;
            }
        }
        return result;
    }

    /**
     * Adds multiple rows
     *
     * @param rows the number of rows to add
     */
    public void addRows(int rows) {
        for (int row = 0; row < rows; row++) {
            table.incrementCounter();
        }
        fireChangedEvent(new TableModelEvent(this));
    }

    /**
     * ImageJ tables break many assumptions about tables, as they lazy-delete their columns without ensuring consecutive IDs
     * This breaks too many algorithms, so re-create the column
     */
    private void cleanupTable() {
        if (table.columnDeleted() || table.getHeadings().length != getColumnNames().size()) {
            ResultsTable original = table;
            table = new ResultsTable(original.getCounter());

            for (String column : original.getHeadings()) {
                Variable[] columnAsVariables = original.getColumnAsVariables(column);
                table.setColumn(column, columnAsVariables);
            }
        }
    }

    /**
     * Returns a column as vector.
     * This is a reference.
     *
     * @param index the column index
     * @return the column
     */
    public TableColumn getColumnReference(int index) {
        return new TableColumnReference(this, index);
    }

    /**
     * Returns a column as vector.
     * This is a copy.
     *
     * @param index the column index
     * @return the column
     */
    public TableColumn getColumnCopy(int index) {
        if (isNumeric(index)) {
            return new DoubleArrayTableColumn(getColumnReference(index).getDataAsDouble(getRowCount()), getColumnName(index));
        } else {
            return new StringArrayTableColumn(getColumnReference(index).getDataAsString(getRowCount()), getColumnName(index));
        }
    }

    @Override
    public void saveTo(Path storageFilePath, String name) {
        try {
            table.saveAs(storageFilePath.resolve(name + ".csv").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new column that consists of a combination of the selected values.
     * They will be formatted like col1=col1row1,col2=col2row1, ...
     *
     * @param sourceColumns the source columns
     * @param newColumn     the new column
     * @param separator     separator character e.g. ", "
     * @param equals        equals character e.g. "="
     */
    public void mergeColumns(List<Integer> sourceColumns, String newColumn, String separator, String equals) {
        String[] value = new String[getRowCount()];
        for (int row = 0; row < getRowCount(); row++) {
            int finalRow = row;
            value[row] = sourceColumns.stream().map(col -> getColumnName(col) + equals + getValueAsString(finalRow, col)).collect(Collectors.joining(separator));
        }
        int col = addColumn(newColumn, true);
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(value[row], row, col);
        }
    }

    /**
     * Duplicates a column
     *
     * @param column    the input column
     * @param newColumn the new column name
     */
    public void duplicateColumn(int column, String newColumn) {
        if (containsColumn(newColumn))
            throw new IllegalArgumentException("Column '" + newColumn + "' already exists!");
        int newColumnIndex = addColumn(newColumn, !isNumeric(column));
        for (int row = 0; row < getRowCount(); row++) {
            setValueAt(getValueAt(row, column), row, newColumnIndex);
        }
    }

    @Override
    public ACAQData duplicate() {
        return new ResultsTableData((ResultsTable) table.clone());
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {
        workbench.getDocumentTabPane().addTab(displayName, UIUtils.getIconFromResources("table.png"),
                new ACAQTableEditor((ACAQProjectWorkbench) workbench, (ResultsTableData) duplicate()), DocumentTabPane.CloseMode.withAskOnCloseButton, true);
        workbench.getDocumentTabPane().switchToLastTab();
    }

    public ResultsTable getTable() {
        return table;
    }

    public void setTable(ResultsTable table) {
        this.table = table;
    }

    /**
     * Returns the index of an existing column
     *
     * @param id the column ID
     * @return the index. -1 if the column does not exist
     */
    public int getColumnIndex(String id) {
        for (int i = 0; i <= table.getLastColumn(); ++i) {
            if (id.equals(table.getColumnHeading(i)))
                return i;
        }
        return -1;
    }

    /**
     * Returns the index of an existing column or creates a new column if it does not exist
     *
     * @param id the column ID
     * @return the column index
     */
    public int getOrCreateColumnIndex(String id) {
        int existing = getColumnIndex(id);
        if (existing == -1) {
            existing = table.getFreeColumn(id);
        }
        return existing;
    }

    /**
     * Adds the table to an existing table
     *
     * @param destination Target table
     */
    public void addToTable(ResultsTable destination) {
        Set<String> existingColumns = new HashSet<>(Arrays.asList(destination.getHeadings()));
        TIntIntMap columnMap = new TIntIntHashMap();
        for (int col = 0; col < getColumnCount(); col++) {
            if (!existingColumns.contains(getColumnName(col))) {
                columnMap.put(col, destination.getFreeColumn(getColumnName(col)));
            } else {
                columnMap.put(col, destination.getColumnIndex(getColumnName(col)));
            }
        }
        int startRow = destination.getCounter();
        for (int row = 0; row < table.size(); ++row) {
            destination.incrementCounter();
            for (int col = 0; col < getColumnCount(); col++) {
                if (isNumeric(col)) {
                    destination.setValue(columnMap.get(col), row + startRow, getValueAsDouble(row, col));
                } else {
                    destination.setValue(columnMap.get(col), row + startRow, getValueAsString(row, col));
                }
            }
        }
    }

    @Override
    public int getRowCount() {
        return table.getCounter();
    }

    @Override
    public int getColumnCount() {
        return table.getLastColumn() + 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return table.getColumnHeading(columnIndex);
    }

    public List<String> getColumnNames() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < getColumnCount(); i++) {
            result.add(getColumnName(i));
        }
        return result;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Hashtable<Integer, ArrayList<Object>> stringColumnsTable = getStringColumnsTable();
        if (stringColumnsTable != null && stringColumnsTable.containsKey(columnIndex)) {
            return String.class;
        } else {
            return Double.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     * Returns a new table that only contains the selected rows.
     * The resulting table is ordered according to the input rows
     *
     * @param rows the row indices
     * @return table only containing the selected rows
     */
    public ResultsTableData getRows(Collection<Integer> rows) {
        ResultsTableData result = new ResultsTableData();

        // Find the location of the columns
        TIntIntMap columnMap = new TIntIntHashMap();
        for (int col = 0; col < getColumnCount(); col++) {
            columnMap.put(col, result.getTable().getFreeColumn(getColumnName(col)));
        }

        int targetRow = 0;
        for (Integer sourceRow : rows) {
            result.table.incrementCounter();
            for (int sourceCol = 0; sourceCol < getColumnCount(); sourceCol++) {
                int targetCol = columnMap.get(sourceCol);
                if (isNumeric(sourceCol)) {
                    result.table.setValue(targetCol, targetRow, getValueAsDouble(sourceRow, sourceCol));
                } else {
                    result.table.setValue(targetCol, targetRow, getValueAsString(sourceRow, sourceCol));
                }
            }
            ++targetRow;
        }

        return result;
    }

    /**
     * @return The table's internal string column table
     */
    private Hashtable<Integer, ArrayList<Object>> getStringColumnsTable() {
        try {
            Field stringColumns = ResultsTable.class.getDeclaredField("stringColumns");
            stringColumns.setAccessible(true);
            return (Hashtable<Integer, ArrayList<Object>>) stringColumns.get(table);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if a column is numeric
     *
     * @param columnIndex the column
     * @return if the column is numeric (type {@link Double}). Otherwise it is a {@link String}
     */
    public boolean isNumeric(int columnIndex) {
        return getColumnClass(columnIndex) == Double.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (isNumeric(columnIndex)) {
            return table.getValueAsDouble(columnIndex, rowIndex);
        } else {
            return table.getStringValue(columnIndex, rowIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue instanceof Number) {
            table.setValue(columnIndex, rowIndex, ((Number) aValue).doubleValue());
        } else {
            table.setValue(columnIndex, rowIndex, "" + aValue);
        }
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this, rowIndex));
        }
    }

    /**
     * Returns a table value as double
     *
     * @param rowIndex    the row
     * @param columnIndex the column
     * @return the table value
     */
    public double getValueAsDouble(int rowIndex, int columnIndex) {
        return table.getValueAsDouble(columnIndex, rowIndex);
    }

    /**
     * Returns a table value as string
     *
     * @param rowIndex    the row
     * @param columnIndex the column
     * @return the table value
     */
    public String getValueAsString(int rowIndex, int columnIndex) {
        return table.getStringValue(columnIndex, rowIndex);
    }

    /**
     * Returns a table value as double
     *
     * @param rowIndex   the row
     * @param columnName the column
     * @return the table value
     */
    public double getValueAsDouble(int rowIndex, String columnName) {
        return table.getValue(columnName, rowIndex);
    }

    /**
     * Returns a table value as string
     *
     * @param rowIndex   the row
     * @param columnName the column
     * @return the table value
     */
    public String getValueAsString(int rowIndex, String columnName) {
        return table.getStringValue(columnName, rowIndex);
    }

    /**
     * Renames a column
     *
     * @param column  the column
     * @param newName the new name
     */
    public void renameColumn(String column, String newName) {
        if (getColumnIndex(column) == -1)
            throw new NullPointerException("Column '" + column + "' does not exist!");
        if (column.equals(newName))
            return;
        if (getColumnIndex(newName) != -1)
            throw new NullPointerException("Column '" + newName + "' already exists!");

        table.renameColumn(column, newName);
    }

    /**
     * Fires a {@link TableModelEvent} to all listeners
     *
     * @param event the event
     */
    public void fireChangedEvent(TableModelEvent event) {
        for (TableModelListener listener : listeners) {
            listener.tableChanged(event);
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    /**
     * Removes the column with the specified index
     *
     * @param col column index
     */
    public void removeColumnAt(int col) {
        table.deleteColumn(getColumnName(col));
        cleanupTable();
        fireChangedEvent(new TableModelEvent(this));
    }

    /**
     * Merges another results table into this one
     *
     * @param other the other data
     */
    public void mergeWith(ResultsTableData other) {
        Map<String, Boolean> inputColumnsNumeric = new HashMap<>();
        for (int col = 0; col < other.getColumnCount(); col++) {
            inputColumnsNumeric.put(other.getColumnName(col), other.isNumeric(col));
        }

        // For some reason ImageJ can create tables with missing columns
        Set<String> allowedColumns = new HashSet<>(Arrays.asList(other.getTable().getHeadings()));

        int localRow = table.getCounter();
        for (int row = 0; row < other.getRowCount(); row++) {
            table.incrementCounter();
            for (int col = 0; col < other.getColumnCount(); col++) {
                String colName = other.getColumnName(col);
                if (!allowedColumns.contains(colName))
                    continue;
                if (inputColumnsNumeric.get(colName)) {
                    table.setValue(colName, localRow, other.getTable().getValueAsDouble(col, row));
                } else {
                    table.setValue(colName, localRow, other.getTable().getStringValue(col, row));
                }
            }

            ++localRow;
        }
        fireChangedEvent(new TableModelEvent(this));
    }

    /**
     * Splits this table into multiple tables according to an internal or external column
     *
     * @param externalColumn the column
     * @return output table map. The map key contains the group.
     */
    public Map<String, ResultsTableData> splitBy(TableColumn externalColumn) {
        String[] groupColumn = externalColumn.getDataAsString(getRowCount());

        Map<String, ResultsTableData> result = new HashMap<>();
        for (int row = 0; row < getRowCount(); row++) {
            String group = groupColumn[row];
            if (group == null)
                group = "";
            ResultsTableData target = result.getOrDefault(group, null);
            if (target == null) {
                target = new ResultsTableData(new ResultsTable());
                result.put(group, target);
            }

            for (int col = 0; col < getColumnCount(); col++) {
                target.getTable().incrementCounter();
                if (isNumeric(col)) {
                    target.getTable().setValue(getColumnName(col), row, table.getStringValue(col, row));
                } else {
                    target.getTable().setValue(getColumnName(col), row, table.getValueAsDouble(col, row));
                }
            }
        }

        return result;
    }

    /**
     * Adds a column with the given name.
     * If the column already exists, the method returns false
     *
     * @param name         column name. cannot be empty.
     * @param stringColumn if the new column is a string column
     * @return the column index (this includes any existing column) or -1 if the creation was not possible
     */
    public int addColumn(String name, boolean stringColumn) {
        if (StringUtils.isNullOrEmpty(name))
            return -1;
        if (getColumnIndex(name) != -1)
            return getColumnIndex(name);
        int index = table.getFreeColumn(name);
        if (stringColumn) {
            for (int row = 0; row < getRowCount(); row++) {
                table.setValue(index, row, "");
            }
        }
        return index;
    }

    /**
     * Converts the column into a string column. Silently ignores columns that are already string columns.
     *
     * @param column column index
     */
    public void convertToStringColumn(int column) {
        if (!isNumeric(column))
            return;
        String[] values = new String[getRowCount()];
        for (int row = 0; row < getRowCount(); row++) {
            values[row] = getValueAsString(row, column);
        }
        String columnName = getColumnName(column);
        table.deleteColumn(columnName);
        column = table.getFreeColumn(columnName);
        for (int row = 0; row < getRowCount(); row++) {
            table.setValue(column, row, values[row]);
        }
        cleanupTable();
    }

    /**
     * Converts the column into a numeric column. Silently ignores columns that are already numeric columns.
     * Attempts to convert string values into their numeric representation. If this fails, defaults to zero.
     *
     * @param column column index
     */
    public void convertToNumericColumn(int column) {
        if (isNumeric(column))
            return;
        double[] values = new double[getRowCount()];
        for (int row = 0; row < getRowCount(); row++) {
            String s = getValueAsString(row, column);
            try {
                values[row] = Double.parseDouble(s);
            } catch (NumberFormatException e) {
            }
        }
        String columnName = getColumnName(column);
        table.deleteColumn(columnName);
        column = table.getFreeColumn(columnName);
        for (int row = 0; row < getRowCount(); row++) {
            table.setValue(column, row, values[row]);
        }
        cleanupTable();
    }

    /**
     * Returns true if the column exists
     *
     * @param columnName the column name
     * @return if the column exists
     */
    public boolean containsColumn(String columnName) {
        return getColumnIndex(columnName) != -1;
    }

    /**
     * Adds a new row
     */
    public void addRow() {
        table.incrementCounter();
        fireChangedEvent(new TableModelEvent(this));
    }

    /**
     * Removes the columns with given headings
     *
     * @param removedColumns the columns to remove
     */
    public void removeColumns(Set<String> removedColumns) {
        for (String removedColumn : removedColumns) {
            table.deleteColumn(removedColumn);
        }
        cleanupTable();
    }

    @Override
    public String toString() {
        return "Table (" + getRowCount() + " rows, " + getColumnCount() + " columns): " + String.join(", ", getColumnNames());
    }

    /**
     * An entry for obtaining statistics/integrated values
     */
    public static class IntegratingColumnOperationEntry {
        private String sourceColumnName;
        private String targetColumnName;
        private IntegratingColumnOperation operation;

        /**
         * Creates a new entry
         *
         * @param sourceColumnName the source column
         * @param targetColumnName the target column
         * @param operation        the operation
         */
        public IntegratingColumnOperationEntry(String sourceColumnName, String targetColumnName, IntegratingColumnOperation operation) {
            this.sourceColumnName = sourceColumnName;
            this.targetColumnName = targetColumnName;
            this.operation = operation;
        }

        public String getSourceColumnName() {
            return sourceColumnName;
        }

        public String getTargetColumnName() {
            return targetColumnName;
        }

        public IntegratingColumnOperation getOperation() {
            return operation;
        }
    }

    /**
     * Points to a cell in the table
     */
    public static class Index {
        public int row;
        public int column;

        /**
         * Creates a new instance. Initialized to zero.
         */
        public Index() {
        }

        /**
         * Creates a new instance and initializes it
         *
         * @param row    the row
         * @param column the column
         */
        public Index(int row, int column) {
            this.row = row;
            this.column = column;
        }

        public int getRow() {
            return row;
        }

        public int getColumn() {
            return column;
        }
    }
}
