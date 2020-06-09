package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.tables.datatypes.DoubleArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.StringArrayTableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumnReference;
import org.hkijena.acaq5.utils.PathUtils;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;

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
     * @param columns key is column heading
     */
    public ResultsTableData(Map<String, TableColumn> columns) {
        importFromColumns(columns);
    }

    /**
     * Creates a {@link ResultsTableData} from a list of columns.
     * Column headings are extracted from the column labels
     * @param columns the columns
     */
    public ResultsTableData(Collection<TableColumn> columns) {
        Map<String, TableColumn> map = new HashMap<>();
        for (TableColumn column : columns) {
            map.put(column.getLabel(), column);
        }
        importFromColumns(map);
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
                if(entry.getValue().isNumeric()) {
                    table.setValue(col, row, entry.getValue().getRowAsDouble(row));
                }
                else {
                    table.setValue(col, row, entry.getValue().getRowAsString(row));
                }
            }
        }
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
     * Returns a column as vector.
     * This is a reference.
     * @param index the column index
     * @return the column
     */
    public TableColumn getColumnReference(int index) {
        return new TableColumnReference(this, index);
    }

    /**
     * Returns a column as vector.
     * This is a copy.
     * @param index the column index
     * @return the column
     */
    public TableColumn getColumnCopy(int index) {
        if(isNumeric(index)) {
            return new DoubleArrayTableColumn(getColumnReference(index).getDataAsDouble(getRowCount()), getColumnName(index));
        }
        else {
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

    @Override
    public ACAQData duplicate() {
        return new ResultsTableData((ResultsTable) table.clone());
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
        for (int row = 0; row < table.size(); ++row) {
            destination.incrementCounter();
            for (int columnIndex = 0; columnIndex < table.getLastColumn(); ++columnIndex) {
                destination.addValue(table.getColumnHeading(columnIndex), table.getValue(table.getColumnHeading(columnIndex), row));
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
        if (getColumnClass(columnIndex) == Double.class) {
            return table.getValueAsDouble(columnIndex, rowIndex);
        } else {
            return table.getStringValue(columnIndex, rowIndex);
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (aValue instanceof String) {
            table.setValue(columnIndex, rowIndex, (String) aValue);
        } else {
            table.setValue(columnIndex, rowIndex, (double) aValue);
        }
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this, rowIndex));
        }
    }

    public double getValueAsDouble(int rowIndex, int columnIndex) {
        return table.getValueAsDouble(columnIndex, rowIndex);
    }

    public String getValueAsString(int rowIndex, int columnIndex) {
        return table.getStringValue(columnIndex, rowIndex);
    }

    public double getValueAsDouble(int rowIndex, String columnName) {
        return table.getValue(columnName, rowIndex);
    }

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
}
