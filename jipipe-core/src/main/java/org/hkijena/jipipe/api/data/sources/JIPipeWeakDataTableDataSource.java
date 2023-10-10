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

package org.hkijena.jipipe.api.data.sources;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataTable;

import java.lang.ref.WeakReference;

/**
 * A {@link JIPipeDataSource}
 */
public class JIPipeWeakDataTableDataSource implements JIPipeDataSource {
    private final WeakReference<JIPipeDataTable> dataTable;
    private final int row;
    private final String dataAnnotation;

    public JIPipeWeakDataTableDataSource(JIPipeDataTable dataTable, int row) {
        this.dataTable = new WeakReference<>(dataTable);
        this.row = row;
        this.dataAnnotation = null;
    }

    public JIPipeWeakDataTableDataSource(JIPipeDataTable dataTable, int row, String dataAnnotation) {
        this.dataTable = new WeakReference<>(dataTable);
        this.row = row;
        this.dataAnnotation = dataAnnotation;
    }

    /**
     * Ensures that a table data source is present. If dataSource is already a table data source, it will be returned.
     * Otherwise, a new {@link JIPipeDataTable} is created and the data is wrapped into it.
     *
     * @param data       the data
     * @param dataSource the data source. can be null.
     * @return a table data source
     */
    public static JIPipeWeakDataTableDataSource wrap(JIPipeData data, JIPipeDataSource dataSource) {
        if (dataSource instanceof JIPipeWeakDataTableDataSource) {
            return (JIPipeWeakDataTableDataSource) dataSource;
        } else {
            JIPipeDataTable table = new JIPipeDataTable(data.getClass());
            table.addData(data, new JIPipeProgressInfo());
            return new JIPipeWeakDataTableDataSource(table, 0);
        }
    }

    /**
     * The data slot where the data is sourced
     *
     * @return the data slot
     */
    public JIPipeDataTable getDataTable() {
        return dataTable.get();
    }

    /**
     * The data slot row where the data is sourced
     *
     * @return the data slot
     */
    public int getRow() {
        return row;
    }

    /**
     * Optional: Data annotation
     *
     * @return the data annotation or null if the main data is referenced
     */
    public String getDataAnnotation() {
        return dataAnnotation;
    }
}
