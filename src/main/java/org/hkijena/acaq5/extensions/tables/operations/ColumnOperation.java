package org.hkijena.acaq5.extensions.tables.operations;

import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

/**
 * An operation on a {@link TableColumn}
 */
public interface ColumnOperation {
    /**
     * Applies the operation
     *
     * @param column the column index
     * @return the result column. The column label is the column heading.
     */
    TableColumn run(TableColumn column);
}
