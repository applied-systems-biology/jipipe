package org.hkijena.acaq5.extensions.imagejdatatypes.datatypes;

import ij.macro.Variable;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;
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

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Hashtable<Integer, ArrayList<Object>> stringColumnsTable = getStringColumnsTable();
        if (stringColumnsTable.containsKey(columnIndex)) {
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
     * @param other the other data
     */
    public void mergeWith(ResultsTableData other) {
        Map<String, Boolean> inputColumnsNumeric = new HashMap<>();
        for (int col = 0; col < other.getColumnCount(); col++) {
            inputColumnsNumeric.put(other.getColumnName(col), other.isNumeric(col));
        }
        int localRow = table.getCounter();
        for (int row = 0; row < other.getRowCount(); row++) {
            table.incrementCounter();
            for (int col = 0; col < other.getColumnCount(); col++) {
                String colName = other.getColumnName(col);
                if(inputColumnsNumeric.get(colName)) {
                    table.setValue(colName, localRow, other.getTable().getValueAsDouble(col, row));
                }
                else {
                    table.setValue(colName, localRow, other.getTable().getStringValue(col, row));
                }
            }

            ++localRow;
        }
        fireChangedEvent(new TableModelEvent(this));
    }
}
