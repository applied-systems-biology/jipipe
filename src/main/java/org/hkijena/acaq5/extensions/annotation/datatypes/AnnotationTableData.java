package org.hkijena.acaq5.extensions.annotation.datatypes;

import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData;
import org.hkijena.acaq5.extensions.tables.datatypes.TableColumn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * A special kind of {@link org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ResultsTableData} that stores annotation columns.
 */
@ACAQDocumentation(name = "Annotation table", description = "A table that contains data annotations and other metadata")
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
     * @param traitDeclaration the annotation type
     * @return index
     */
    public int addAnnotationColumn(ACAQTraitDeclaration traitDeclaration) {
        return addColumn(getAnnotationColumnName(traitDeclaration), true);
    }

    /**
     * Returns all columns that do not identify as annotation (via 'annotation:' prefix)
     * @return all columns that do not identify as annotation
     */
    public Set<String> getMetadataColumns() {
        Set<String> result = new HashSet<>();
        for (String columnName : getColumnNames()) {
            if(!columnName.startsWith("annotation:")) {
                result.add(columnName);
            }
        }
        return result;
    }

    /**
     * Returns all columns that reference valid annotation types
     * @return all column names that reference valid annotation types
     */
    public Set<String> getAnnotationColumns() {
        Set<String> result = new HashSet<>();
        for (String columnName : getColumnNames()) {
            ACAQTraitDeclaration annotationType = getAnnotationTypeFromColumnName(columnName);
            if(annotationType != null) {
                result.add(columnName);
            }
        }
        return result;
    }

    /**
     * Returns the annotation column name of the annotation type
     * @param traitDeclaration the annotation type
     * @return column name
     */
    public static String getAnnotationColumnName(ACAQTraitDeclaration traitDeclaration) {
        return "annotation:" + traitDeclaration.getId();
    }

    /**
     * Returns the trait declaration from a column name. If the annotation type does not exist or
     * the column is not an annotation column, null is returned
     * @param columnName the column name
     * @return annotation type or null if not an annotation column name or the annotation type does not exist
     */
    public static ACAQTraitDeclaration getAnnotationTypeFromColumnName(String columnName) {
        if(columnName.startsWith("annotation:")) {
            String id = columnName.substring("annotation:".length());
            if(ACAQTraitRegistry.getInstance().hasTraitWithId(id))
                return ACAQTraitRegistry.getInstance().getDeclarationById(id);
        }
        return null;
    }

    @Override
    public String toString() {
        return "Annotation table (" + getRowCount() + "x" + getColumnCount() + ")";
    }
}
