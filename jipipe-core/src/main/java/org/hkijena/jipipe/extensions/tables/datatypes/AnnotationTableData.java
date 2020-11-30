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
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A special kind of {@link ResultsTableData} that stores annotation columns.
 */
@JIPipeDocumentation(name = "Annotation table", description = "A table that contains data annotations and other metadata")
public class AnnotationTableData extends ResultsTableData {

    /**
     * {@inheritDoc}
     */
    public AnnotationTableData() {
    }

    /**
     * {@inheritDoc}
     */
    public AnnotationTableData(Map<String, TableColumn> columns) {
        super(columns);
    }

    /**
     * {@inheritDoc}
     */
    public AnnotationTableData(Collection<TableColumn> columns) {
        super(columns);
    }

    /**
     * {@inheritDoc}
     */
    public AnnotationTableData(ResultsTable table) {
        super(table);
    }

    /**
     * {@inheritDoc}
     */
    public AnnotationTableData(ResultsTableData other) {
        super(other);
    }

    /**
     * Adds a new column that contains annotation data and returns its index-
     * If the column already exists, its index is returned.
     *
     * @param traitInfo the annotation type
     * @return index
     */
    public int addAnnotationColumn(String traitInfo) {
        return addColumn(traitInfo, true);
    }

    /**
     * Gets all annotations at the specified row
     *
     * @param row the row
     * @return annotations
     */
    public List<JIPipeAnnotation> getAnnotations(int row) {
        List<JIPipeAnnotation> result = new ArrayList<>();
        for (int col = 0; col < getColumnCount(); col++) {
            String columnName = getColumnName(col);
            result.add(new JIPipeAnnotation(columnName, StringUtils.orElse(getValueAsString(row, col), "")));
        }
        return result;
    }

    @Override
    public String toString() {
        return "Annotation table (" + getRowCount() + "x" + getColumnCount() + ")";
    }

    public static AnnotationTableData importFrom(Path storageFolder) {
        return new AnnotationTableData(ResultsTableData.importFrom(storageFolder));
    }
}
