package org.hkijena.acaq5.extensions.parameters.collections;

import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterVisibility;

import java.lang.annotation.Annotation;

/**
 * Implements the access to table cell
 */
public class ParameterTableCellAccess implements ACAQParameterAccess {

    private ACAQParameterAccess parent;
    private ParameterTable table;
    private int row;
    private int column;

    /**
     * Creates a new instance
     *
     * @param parent the parent access
     * @param table  the table
     * @param row    the row
     * @param column the column
     */
    public ParameterTableCellAccess(ACAQParameterAccess parent, ParameterTable table, int row, int column) {
        this.parent = parent;
        this.table = table;
        this.row = row;
        this.column = column;
    }

    @Override
    public String getKey() {
        return parent.getKey() + "/" + row + "," + column;
    }

    @Override
    public String getName() {
        return table.getColumnName(column);
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public ACAQParameterVisibility getVisibility() {
        return ACAQParameterVisibility.TransitiveVisible;
    }

    @Override
    public <T extends Annotation> T getAnnotationOfType(Class<T> klass) {
        return null;
    }

    @Override
    public Class<?> getFieldClass() {
        return table.getColumnClass(column);
    }

    @Override
    public <T> T get() {
        return (T) table.getValueAt(row, column);
    }

    @Override
    public <T> boolean set(T value) {
        table.setValueAt(value, row, column);
        return true;
    }

    @Override
    public ACAQParameterCollection getSource() {
        return parent.getSource();
    }

    @Override
    public double getPriority() {
        return 0;
    }

    @Override
    public String getShortKey() {
        return getKey();
    }

    @Override
    public int getUIOrder() {
        return 0;
    }
}
