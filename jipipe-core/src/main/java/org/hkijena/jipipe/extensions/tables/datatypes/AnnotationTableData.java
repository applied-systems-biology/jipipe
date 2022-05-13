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

package org.hkijena.jipipe.extensions.tables.datatypes;

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A special kind of {@link ResultsTableData} that stores annotation columns.
 */
@JIPipeDocumentation(name = "Annotation table", description = "A table that contains data annotations and other metadata")
public class AnnotationTableData extends ResultsTableData {

    public AnnotationTableData() {
    }

    public AnnotationTableData(Map<String, TableColumn> columns) {
        super(columns);
    }

    public AnnotationTableData(Collection<TableColumn> columns) {
        super(columns);
    }

    public AnnotationTableData(ResultsTable table) {
        super(table);
    }

    public AnnotationTableData(ResultsTableData other) {
        super(other);
    }

    public static AnnotationTableData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return new AnnotationTableData(ResultsTableData.importData(storage, progressInfo));
    }

    /**
     * Adds a new column that contains annotation data and returns its index-
     * If the column already exists, its index is returned.
     *
     * @param name the annotation type
     * @return index
     */
    public int addAnnotationColumn(String name) {
        return addColumn(name, true);
    }

    /**
     * Gets all annotations at the specified row
     *
     * @param row the row
     * @return annotations
     */
    public List<JIPipeTextAnnotation> getAnnotations(int row) {
        List<JIPipeTextAnnotation> result = new ArrayList<>();
        for (int col = 0; col < getColumnCount(); col++) {
            String columnName = getColumnName(col);
            result.add(new JIPipeTextAnnotation(columnName, StringUtils.orElse(getValueAsString(row, col), "")));
        }
        return result;
    }

    @Override
    public String toString() {
        return "Annotation table (" + getRowCount() + "x" + getColumnCount() + ")";
    }
}
