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
}
