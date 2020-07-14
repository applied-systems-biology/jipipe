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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
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
    public AnnotationTableData(Path storageFilePath) throws IOException {
        super(storageFilePath);
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
        return addColumn(getAnnotationColumnName(traitInfo), true);
    }

    /**
     * Returns all columns that do not identify as annotation (via 'annotation:' prefix)
     *
     * @return all columns that do not identify as annotation
     */
    public Set<String> getMetadataColumns() {
        Set<String> result = new HashSet<>();
        for (String columnName : getColumnNames()) {
            if (!columnName.startsWith("annotation:")) {
                result.add(columnName);
            }
        }
        return result;
    }

    /**
     * Returns all columns that reference valid annotation types
     *
     * @return all column names that reference valid annotation types
     */
    public Set<String> getAnnotationColumns() {
        Set<String> result = new HashSet<>();
        for (String columnName : getColumnNames()) {
            String annotationType = getAnnotationTypeFromColumnName(columnName);
            if (annotationType != null) {
                result.add(columnName);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Annotation table (" + getRowCount() + "x" + getColumnCount() + ")";
    }

    /**
     * Returns the annotation column name of the annotation type
     *
     * @param traitInfo the annotation type
     * @return column name
     */
    public static String getAnnotationColumnName(String traitInfo) {
        return "annotation:" + traitInfo;
    }

    /**
     * Returns the trait info from a column name. If the annotation type does not exist or
     * the column is not an annotation column, null is returned
     *
     * @param columnName the column name
     * @return annotation type or null if not an annotation column name or the annotation type does not exist
     */
    public static String getAnnotationTypeFromColumnName(String columnName) {
        if (columnName.startsWith("annotation:")) {
            return columnName.substring("annotation:".length());
        }
        return null;
    }
}
