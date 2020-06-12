package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

import java.util.function.Function;

/**
 * An operation on a {@link TableColumn}
 */
public interface ColumnOperation extends Function<TableColumn, TableColumn> {
    /**
     * Applies the operation
     * @param column the column index
     * @return the result column. The column label is the column heading.
     */
    TableColumn apply(TableColumn column);
}
