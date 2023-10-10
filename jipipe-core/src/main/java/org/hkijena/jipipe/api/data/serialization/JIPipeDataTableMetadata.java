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

package org.hkijena.jipipe.api.data.serialization;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeExportedDataAnnotation;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contains all metadata exported from an {@link JIPipeDataTable}
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public class JIPipeDataTableMetadata implements TableModel, List<JIPipeDataTableMetadataRow> {
    private Class<? extends JIPipeData> acceptedDataType;
    private List<JIPipeDataTableMetadataRow> rowList = new ArrayList<>();
    private List<String> annotationColumns;
    private List<String> dataAnnotationColumns;

    public JIPipeDataTableMetadata() {
    }

    /**
     * Loads the table from JSON
     *
     * @param fileName JSON file
     * @return Loaded table
     */
    public static JIPipeDataTableMetadata loadFromJson(Path fileName) {
        try {
            JIPipeDataTableMetadata result = JsonUtils.getObjectMapper().readerFor(JIPipeDataTableMetadata.class).readValue(fileName.toFile());
            for (JIPipeDataTableMetadataRow row : result.getRowList()) {
                for (JIPipeExportedDataAnnotation dataAnnotation : row.getDataAnnotations()) {
                    dataAnnotation.setTableRow(row);
                }
            }
            return result;
        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(e,
                    "Unable to load data table from '" + fileName + "'!",
                    "Either the file is inaccessible, or corrupt.",
                    "Check if the file is readable and contains valid JSON data.");
        }
    }

    /**
     * Returns the format version for storage in the JSON file.
     * This might be used later to allow backward-compatibility
     *
     * @return the format version
     */
    @JsonGetter("jipipe:data-table-format-version")
    public int getFormatVersion() {
        return 1;
    }

    /**
     * @return The accepted datatype ID
     */
    @JsonGetter("data-type")
    public String getAcceptedDataTypeId() {
        return JIPipe.getDataTypes().getIdOf(acceptedDataType);
    }

    /**
     * Sets the accepted datatype ID
     *
     * @param id Datatype ID
     */
    @JsonSetter("data-type")
    public void setAcceptedDataTypeId(String id) {
        this.acceptedDataType = JIPipe.getDataTypes().getById(id);
    }

    /**
     * @return List of rows
     */
    @JsonGetter("rows")
    public List<JIPipeDataTableMetadataRow> getRowList() {
        return Collections.unmodifiableList(rowList);
    }

    /**
     * Sets list of rows
     *
     * @param rowList Row list
     */
    @JsonSetter("rows")
    public void setRowList(List<JIPipeDataTableMetadataRow> rowList) {
        this.rowList = rowList;
        this.annotationColumns = null;
        this.dataAnnotationColumns = null;
    }

    /**
     * Saves the table to JSON
     *
     * @param fileName JSON file
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public void saveAsJson(Path fileName) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    /**
     * Gets the storage path of the specified row
     *
     * @param storagePath the storage path that contains the data table
     * @param row         the row index
     * @return the path pointing to the data for the row
     */
    public Path getRowStoragePath(Path storagePath, int row) {
        return storagePath.resolve("" + row);
    }

    /**
     * Saves the table to CSV
     *
     * @param fileName CSV file
     * @throws IOException Triggered by {@link ResultsTable}
     */
    public void saveAsCSV(Path fileName) throws IOException {
        ResultsTableData table = new ResultsTableData();
        for (JIPipeDataTableMetadataRow row : rowList) {
            Map<String, Object> tableRow = new HashMap<>();
            tableRow.put("jipipe:data-type", JIPipe.getDataTypes().getIdOf(acceptedDataType));
            tableRow.put("jipipe:true-data-type", row.getTrueDataType());
            tableRow.put("jipipe:index", row.getIndex());
            tableRow.put("jipipe:data-context", JsonUtils.toJsonString(row.getDataContext()));
            for (String dataAnnotationColumn : getDataAnnotationColumns()) {
                JIPipeExportedDataAnnotation existing = row.getDataAnnotations().stream().filter(t -> t.nameEquals(dataAnnotationColumn)).findFirst().orElse(null);
                if (existing != null)
                    tableRow.put("$" + dataAnnotationColumn, existing.getRowStorageFolder().toString() + " [" + existing.getTrueDataType() + "]");
                else
                    tableRow.put("$" + dataAnnotationColumn, "");
            }
            for (String annotationColumn : getAnnotationColumns()) {
                JIPipeTextAnnotation existing = row.getTextAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
                if (existing != null)
                    tableRow.put(annotationColumn, existing.getValue());
                else
                    tableRow.put(annotationColumn, "");
            }
            table.addRow(tableRow);
        }
        table.saveAsCSV(fileName);
    }

    public List<String> getDataAnnotationColumns() {
        if (dataAnnotationColumns == null) {
            Set<String> registeredAnnotations = new HashSet<>();
            for (JIPipeDataTableMetadataRow row : rowList) {
                registeredAnnotations.addAll(row.getDataAnnotations().stream().map(JIPipeExportedDataAnnotation::getName).collect(Collectors.toSet()));
            }
            dataAnnotationColumns = new ArrayList<>(registeredAnnotations);
        }
        return dataAnnotationColumns;
    }

    /**
     * @return Additional columns
     */
    public List<String> getAnnotationColumns() {
        if (annotationColumns == null) {
            Set<String> registeredAnnotations = new HashSet<>();
            for (JIPipeDataTableMetadataRow row : rowList) {
                registeredAnnotations.addAll(row.getTextAnnotations().stream().map(JIPipeTextAnnotation::getName).collect(Collectors.toSet()));
            }
            annotationColumns = new ArrayList<>(registeredAnnotations);
        }
        return annotationColumns;
    }

    @Override
    public int getRowCount() {
        return rowList.size();
    }

    @Override
    public int getColumnCount() {
        return getAnnotationColumns().size() + getDataAnnotationColumns().size() + 3;
    }

    /**
     * Converts the column index to an annotation column index, or returns -1 if the column is not one
     *
     * @param columnIndex absolute column index
     * @return relative annotation column index, or -1
     */
    public int toAnnotationColumnIndex(int columnIndex) {
        if (columnIndex >= getDataAnnotationColumns().size() + 3)
            return columnIndex - getDataAnnotationColumns().size() - 3;
        else
            return -1;
    }

    /**
     * Converts the column index to a data annotation column index, or returns -1 if the column is not one
     *
     * @param columnIndex absolute column index
     * @return relative data annotation column index, or -1
     */
    public int toDataAnnotationColumnIndex(int columnIndex) {
        if (columnIndex < getDataAnnotationColumns().size() + 3 && (columnIndex - 3) < getDataAnnotationColumns().size()) {
            return columnIndex - 3;
        } else {
            return -1;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Index";
        else if (columnIndex == 1)
            return "Data type";
        else if (columnIndex == 2) {
            return "Preview";
        } else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            return "$" + getDataAnnotationColumns().get(toDataAnnotationColumnIndex(columnIndex));
        } else {
            return getAnnotationColumns().get(toAnnotationColumnIndex(columnIndex));
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return Path.class;
        else if (columnIndex == 1)
            return JIPipeDataInfo.class;
        else if (columnIndex == 2)
            return JIPipeDataTableMetadataRow.class;
        else if (toDataAnnotationColumnIndex(columnIndex) != -1)
            return JIPipeExportedDataAnnotation.class;
        else
            return JIPipeTextAnnotation.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0)
            return rowList.get(rowIndex).getIndex();
        else if (columnIndex == 1) {
            return getDataTypeOf(rowIndex);
        } else if (columnIndex == 2) {
            return rowList.get(rowIndex);
        } else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            String annotationColumn = dataAnnotationColumns.get(toDataAnnotationColumnIndex(columnIndex));
            return rowList.get(rowIndex).getDataAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
        } else {
            String annotationColumn = annotationColumns.get(toAnnotationColumnIndex(columnIndex));
            return rowList.get(rowIndex).getTextAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
        }
    }

    /**
     * Returns the true data type (if available) or the accepted data type of the given row
     *
     * @param rowIndex the row
     * @return the data type
     */
    public JIPipeDataInfo getDataTypeOf(int rowIndex) {
        String trueDataType = rowList.get(rowIndex).getTrueDataType();
        if (!StringUtils.isNullOrEmpty(trueDataType))
            return JIPipeDataInfo.getInstance(trueDataType);
        else
            return JIPipeDataInfo.getInstance(acceptedDataType);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

    }

    @Override
    public void addTableModelListener(TableModelListener l) {

    }

    @Override
    public void removeTableModelListener(TableModelListener l) {

    }

    /**
     * Converts the data table into an annotation table
     *
     * @return the table
     */
    public AnnotationTableData toAnnotationTable() {
        AnnotationTableData output = new AnnotationTableData();
        int outputRow = 0;
        for (JIPipeDataTableMetadataRow row : getRowList()) {
            output.addRow();
            for (JIPipeTextAnnotation annotation : row.getTextAnnotations()) {
                if (annotation != null) {
                    int col = output.addAnnotationColumn(annotation.getName());
                    output.setValueAt(annotation.getValue(), outputRow, col);
                }
            }
            ++outputRow;
        }
        return output;
    }

    @Override
    public int size() {
        return rowList.size();
    }

    @Override
    public boolean isEmpty() {
        return rowList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return rowList.contains(o);
    }

    @NotNull
    @Override
    public Iterator<JIPipeDataTableMetadataRow> iterator() {
        return rowList.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return rowList.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return rowList.toArray(a);
    }

    @Override
    public boolean add(JIPipeDataTableMetadataRow row) {
        rowList.add(row);
        dataAnnotationColumns = null;
        annotationColumns = null;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (rowList.remove(o)) {
            dataAnnotationColumns = null;
            annotationColumns = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return rowList.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends JIPipeDataTableMetadataRow> c) {
        rowList.addAll(c);
        dataAnnotationColumns = null;
        annotationColumns = null;
        return true;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends JIPipeDataTableMetadataRow> c) {
        rowList.addAll(index, c);
        dataAnnotationColumns = null;
        annotationColumns = null;
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (rowList.removeAll(c)) {
            dataAnnotationColumns = null;
            annotationColumns = null;
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        if (rowList.retainAll(c)) {
            dataAnnotationColumns = null;
            annotationColumns = null;
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        rowList.clear();
        dataAnnotationColumns = null;
        annotationColumns = null;
    }

    @Override
    public JIPipeDataTableMetadataRow get(int index) {
        return rowList.get(index);
    }

    @Override
    public JIPipeDataTableMetadataRow set(int index, JIPipeDataTableMetadataRow element) {
        JIPipeDataTableMetadataRow result = rowList.set(index, element);
        dataAnnotationColumns = null;
        annotationColumns = null;
        return result;
    }

    @Override
    public void add(int index, JIPipeDataTableMetadataRow element) {
        rowList.add(index, element);
        dataAnnotationColumns = null;
        annotationColumns = null;
    }

    @Override
    public JIPipeDataTableMetadataRow remove(int index) {
        JIPipeDataTableMetadataRow result = rowList.remove(index);
        dataAnnotationColumns = null;
        annotationColumns = null;
        return result;
    }

    @Override
    public int indexOf(Object o) {
        return rowList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return 0;
    }

    @NotNull
    @Override
    public ListIterator<JIPipeDataTableMetadataRow> listIterator() {
        return rowList.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<JIPipeDataTableMetadataRow> listIterator(int index) {
        return rowList.listIterator(index);
    }

    @NotNull
    @Override
    public List<JIPipeDataTableMetadataRow> subList(int fromIndex, int toIndex) {
        return rowList.subList(fromIndex, toIndex);
    }
}
