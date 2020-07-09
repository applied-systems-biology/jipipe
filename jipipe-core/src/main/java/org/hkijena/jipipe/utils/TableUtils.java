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

package org.hkijena.jipipe.utils;

import javax.swing.table.DefaultTableModel;
import java.util.Vector;

/**
 * Utilities for {@link javax.swing.table.TableModel}
 */
public class TableUtils {
    private TableUtils() {

    }

    /**
     * Copies a table
     *
     * @param tableModel the table
     * @return the copy
     */
    public static DefaultTableModel cloneTableModel(DefaultTableModel tableModel) {
        DefaultTableModel copy = new DefaultTableModel();
        copy.setColumnCount(tableModel.getColumnCount());
        {
            Object[] identifiers = new Object[tableModel.getColumnCount()];
            for (int i = 0; i < tableModel.getColumnCount(); ++i) {
                identifiers[i] = tableModel.getColumnName(i);
            }
            copy.setColumnIdentifiers(identifiers);
        }
        for (int row = 0; row < tableModel.getRowCount(); ++row) {
            Vector<Object> rowVector = new Vector<>(tableModel.getColumnCount());
            for (int column = 0; column < tableModel.getColumnCount(); ++column) {
                rowVector.add(tableModel.getValueAt(row, column));
            }
            copy.addRow(rowVector);
        }
        return copy;
    }

    /**
     * Gets the table's column names
     *
     * @param tableModel the table
     * @return the column names
     */
    public static Vector<String> getColumnIdentifiers(DefaultTableModel tableModel) {
        Vector<String> vector = new Vector<>(tableModel.getColumnCount());
        for (int i = 0; i < tableModel.getColumnCount(); ++i) {
            vector.add(tableModel.getColumnName(i));
        }
        return vector;
    }

    /**
     * Adds a name to columns
     *
     * @param tableModel the table
     * @param name       the new column
     */
    public static void addNameToColumnIdentifiers(DefaultTableModel tableModel, String name) {
        Vector<String> identifiers = getColumnIdentifiers(tableModel);
        for (int i = 0; i < identifiers.size(); ++i) {
            identifiers.set(i, name + "." + identifiers.get(i));
        }
        tableModel.setColumnIdentifiers(identifiers);
    }
}
