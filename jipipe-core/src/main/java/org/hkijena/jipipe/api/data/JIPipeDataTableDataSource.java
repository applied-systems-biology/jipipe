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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

/**
 * A {@link JIPipeDataSource}
 */
public class JIPipeDataTableDataSource implements JIPipeDataSource {
    private final JIPipeDataTable dataTable;
    private final int row;
    private final String dataAnnotation;

    public JIPipeDataTableDataSource(JIPipeDataTable dataTable, int row) {
        this.dataTable = dataTable;
        this.row = row;
        this.dataAnnotation = null;
    }

    public JIPipeDataTableDataSource(JIPipeDataTable dataTable, int row, String dataAnnotation) {
        this.dataTable = dataTable;
        this.row = row;
        this.dataAnnotation = dataAnnotation;
    }

    /**
     * The data slot where the data is sourced
     *
     * @return the data slot
     */
    public JIPipeDataTable getDataTable() {
        return dataTable;
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

    /**
     * Ensures that a table data source is present. If dataSource is already a table data source, it will be returned.
     * Otherwise, a new {@link JIPipeDataTable} is created and the data is wrapped into it.
     * @param data the data
     * @param dataSource the data source. can be null.
     * @return a table data source
     */
    public static JIPipeDataTableDataSource wrap(JIPipeData data, JIPipeDataSource dataSource) {
        if(dataSource instanceof JIPipeDataTableDataSource) {
            return (JIPipeDataTableDataSource) dataSource;
        }
        else {
            JIPipeDataTable table = new JIPipeDataTable(data.getClass());
            table.addData(data, new JIPipeProgressInfo());
            return new JIPipeDataTableDataSource(table, 0);
        }
    }
}
