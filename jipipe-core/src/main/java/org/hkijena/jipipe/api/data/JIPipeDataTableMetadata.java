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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.measure.ResultsTable;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains all metadata exported from an {@link JIPipeDataTable}
 */
public class JIPipeDataTableMetadata implements TableModel {
    private Class<? extends JIPipeData> acceptedDataType;
    private List<JIPipeDataTableMetadataRow> rowList;
    private List<String> annotationColumns;
    private List<String> dataAnnotationColumns;



    /**
     * Initializes a new table from a {@link JIPipeDataTable}
     *
     * @param dataTable    The slot
     */
    public JIPipeDataTableMetadata(JIPipeDataTable dataTable) {
        this.acceptedDataType = dataTable.getAcceptedDataType();
        this.rowList = new ArrayList<>();
        List<String> dataAnnotationColumns = dataTable.getDataAnnotationColumns();
        for (int row = 0; row < dataTable.getRowCount(); ++row) {
            JIPipeDataTableMetadataRow rowInstance = new JIPipeDataTableMetadataRow();
            rowInstance.setIndex(row);
            rowInstance.setAnnotations(dataTable.getTextAnnotations(row));
            rowInstance.setTrueDataType(JIPipeDataInfo.getInstance(dataTable.getDataClass(row)).getId());
            for (int i = 0; i < dataAnnotationColumns.size(); i++) {
                JIPipeVirtualData virtualDataAnnotation = dataTable.getVirtualDataAnnotation(row, dataAnnotationColumns.get(i));
                if (virtualDataAnnotation != null) {
                    JIPipeExportedDataAnnotation exportedDataAnnotation = new JIPipeExportedDataAnnotation();
                    exportedDataAnnotation.setName(dataAnnotationColumns.get(i));
                    exportedDataAnnotation.setRowStorageFolder(Paths.get("_" + i).resolve("" + row));
                    exportedDataAnnotation.setTrueDataType(JIPipeDataInfo.getInstance(virtualDataAnnotation.getDataClass()).getId());
                    exportedDataAnnotation.setTableRow(rowInstance);
                    rowInstance.getDataAnnotations().add(exportedDataAnnotation);
                }
            }
            rowList.add(rowInstance);
        }
    }

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
            throw new UserFriendlyRuntimeException(e, "Unable to load data table from '" + fileName + "'!",
                    "Load JIPipe results", "Either the file is inaccessible, or corrupt.",
                    "Check if the file is readable and contains valid JSON data.");
        }
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
        return rowList;
    }

    /**
     * Sets list of rows
     *
     * @param rowList Row list
     */
    @JsonSetter("rows")
    public void setRowList(List<JIPipeDataTableMetadataRow> rowList) {
        this.rowList = rowList;
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
        ResultsTable table = new ResultsTable();
        for (JIPipeDataTableMetadataRow row : rowList) {
            table.incrementCounter();
            table.addValue("jipipe:data-type", JIPipe.getDataTypes().getIdOf(acceptedDataType));
            table.addValue("jipipe:true-data-type", row.getTrueDataType());
            table.addValue("jipipe:index", row.getIndex());
            for (String dataAnnotationColumn : getDataAnnotationColumns()) {
                JIPipeExportedDataAnnotation existing = row.getDataAnnotations().stream().filter(t -> t.nameEquals(dataAnnotationColumn)).findFirst().orElse(null);
                if (existing != null)
                    table.addValue(dataAnnotationColumn, existing.getRowStorageFolder().toString() + " [" + existing.getTrueDataType() + "]");
                else
                    table.addValue(dataAnnotationColumn, "");
            }
            for (String annotationColumn : getAnnotationColumns()) {
                JIPipeTextAnnotation existing = row.getAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
                if (existing != null)
                    table.addValue(annotationColumn, existing.getValue());
                else
                    table.addValue(annotationColumn, "");
            }
        }
        table.saveAs(fileName.toString());
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
                registeredAnnotations.addAll(row.getAnnotations().stream().map(JIPipeTextAnnotation::getName).collect(Collectors.toSet()));
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
            return rowList.get(rowIndex).getAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
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
            for (JIPipeTextAnnotation annotation : row.getAnnotations()) {
                if (annotation != null) {
                    int col = output.addAnnotationColumn(annotation.getName());
                    output.setValueAt(annotation.getValue(), outputRow, col);
                }
            }
            ++outputRow;
        }
        return output;
    }

}
