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

package org.hkijena.jipipe.plugins.tables;

import org.hkijena.jipipe.plugins.tables.datatypes.TableColumnData;

import java.util.function.Function;

/**
 * An operation on a {@link TableColumnData}
 */
public interface ColumnOperation extends Function<TableColumnData, TableColumnData> {
    /**
     * Applies the operation
     *
     * @param column the column index
     * @return the result column. The column label is the column heading.
     */
    TableColumnData apply(TableColumnData column);
}
