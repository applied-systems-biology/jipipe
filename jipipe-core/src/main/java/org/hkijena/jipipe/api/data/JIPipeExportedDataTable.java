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
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.tables.datatypes.AnnotationTableData;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains all metadata exported from an {@link JIPipeDataSlot}
 * TODO: Support for data annotations
 */
public class JIPipeExportedDataTable implements TableModel {
    private String nodeId;
    private String slotName;
    private String internalPath;
    private Class<? extends JIPipeData> acceptedDataType;
    private List<Row> rowList;
    private List<String> annotationColumns;

    /**
     * Initializes a new table from a slot
     *
     * @param slot    The slot
     * @param indices output path index for each slot row
     */
    public JIPipeExportedDataTable(JIPipeDataSlot slot, Path basePath, List<Integer> indices) {
        this.nodeId = slot.getNode().getInfo().getId();
        this.slotName = slot.getName();
        if (basePath != null) {
            this.internalPath = basePath.relativize(slot.getStoragePath()).toString();
        } else if (slot.getStoragePath() != null) {
            this.internalPath = slot.getStoragePath().toString();
        } else {
            this.internalPath = "";
        }
        this.acceptedDataType = slot.getAcceptedDataType();
        this.rowList = new ArrayList<>();
        for (int row = 0; row < slot.getRowCount(); ++row) {
            Row rowInstance = new Row();
            rowInstance.index = indices.get(row);
            rowInstance.annotations = slot.getAnnotations(row);
            rowInstance.trueDataType = JIPipeDataInfo.getInstance(slot.getDataClass(row)).getId();
            rowList.add(rowInstance);
        }
    }

    public JIPipeExportedDataTable() {
    }

    /**
     * @return Gets the algorithm ID
     */
    @JsonGetter("node-id")
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Sets the algorithm ID
     *
     * @param nodeId the algorithm ID
     */
    @JsonSetter("node-id")
    private void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Compatibility function to allow reading tables in an older format
     *
     * @param nodeId the algorithm ID
     */
    @JsonSetter("algorithm-id")
    private void setAlgorithmId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return The slot name
     */
    @JsonGetter("slot")
    public String getSlotName() {
        return slotName;
    }

    /**
     * Sets the slot name
     *
     * @param slotName The slot name
     */
    @JsonSetter("slot")
    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    /**
     * @return The internal path
     */
    @JsonGetter("internal-path")
    public String getInternalPath() {
        return internalPath;
    }

    /**
     * Sets the internal path
     *
     * @param internalPath The internal path
     */
    @JsonSetter("internal-path")
    public void setInternalPath(String internalPath) {
        this.internalPath = internalPath;
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
    public List<Row> getRowList() {
        return rowList;
    }

    /**
     * Sets list of rows
     *
     * @param rowList Row list
     */
    @JsonSetter("rows")
    public void setRowList(List<Row> rowList) {
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
        for (Row row : rowList) {
            table.incrementCounter();
            table.addValue("jipipe:node-id", nodeId);
            table.addValue("jipipe:slot", slotName);
            table.addValue("jipipe:data-type", JIPipe.getDataTypes().getIdOf(acceptedDataType));
            table.addValue("jipipe:true-data-type", row.trueDataType);
            table.addValue("jipipe:internal-path", internalPath);
            table.addValue("jipipe:index", row.index);
            for (String annotationColumn : getAnnotationColumns()) {
                JIPipeAnnotation existing = row.annotations.stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
                if (existing != null)
                    table.addValue(annotationColumn, existing.getValue());
                else
                    table.addValue(annotationColumn, "");
            }
        }
        table.saveAs(fileName.toString());
    }

    /**
     * @return Additional columns
     */
    public List<String> getAnnotationColumns() {
        if (annotationColumns == null) {
            Set<String> registeredAnnotations = new HashSet<>();
            for (Row row : rowList) {
                registeredAnnotations.addAll(row.annotations.stream().map(JIPipeAnnotation::getName).collect(Collectors.toSet()));
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
        return getAnnotationColumns().size() + 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Index";
        else if (columnIndex == 1)
            return "Data type";
        else if (columnIndex == 2) {
            return "Preview";
        } else
            return annotationColumns.get(columnIndex - 3);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return Path.class;
        else if (columnIndex == 1)
            return JIPipeDataInfo.class;
        else if (columnIndex == 2)
            return Row.class;
        else
            return JIPipeAnnotation.class;
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
        } else if (columnIndex == 2)
            return rowList.get(rowIndex);
        else {
            String annotationColumn = annotationColumns.get(columnIndex - 3);
            return rowList.get(rowIndex).annotations.stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
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
     * @return the table
     */
    public AnnotationTableData toAnnotationTable() {
        AnnotationTableData output = new AnnotationTableData();
        int outputRow = 0;
        for (Row row : getRowList()) {
            output.addRow();
            for (JIPipeAnnotation annotation : row.getAnnotations()) {
                if (annotation != null) {
                    int col = output.addAnnotationColumn(annotation.getName());
                    output.setValueAt(annotation.getValue(), outputRow, col);
                }
            }
            ++outputRow;
        }
        return output;
    }

    /**
     * Loads the table from JSON
     *
     * @param fileName JSON file
     * @return Loaded table
     */
    public static JIPipeExportedDataTable loadFromJson(Path fileName) {
        try {
            return JsonUtils.getObjectMapper().readerFor(JIPipeExportedDataTable.class).readValue(fileName.toFile());
        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Unable to load data table from '" + fileName + "'!",
                    "Load JIPipe results", "Either the file is inaccessible, or corrupt.",
                    "Check if the file is readable and contains valid JSON data.");
        }
    }

    /**
     * A row in the table
     */
    public static class Row {
        private int index;
        private List<JIPipeAnnotation> annotations;
        private String trueDataType;

        /**
         * Creates new instance
         */
        public Row() {
        }

        /**
         * @return Internal location relative to the output folder
         */
        @JsonGetter("index")
        public int getIndex() {
            return index;
        }

        /**
         * Sets the location
         *
         * @param index Internal location relative to the output folder
         */
        @JsonSetter("index")
        public void setIndex(int index) {
            this.index = index;
        }

        /**
         * @return Annotations
         */
        @JsonGetter("annotations")
        public List<JIPipeAnnotation> getAnnotations() {
            return annotations;
        }

        /**
         * Sets annotations
         *
         * @param annotations List of annotations
         */
        @JsonSetter("annotations")
        public void setAnnotations(List<JIPipeAnnotation> annotations) {
            this.annotations = annotations;
        }

        /**
         * Compatibility function to allow reading tables in an older format
         *
         * @param annotations List of annotations
         */
        @JsonSetter("traits")
        public void setTraits(List<JIPipeAnnotation> annotations) {
            this.annotations = annotations;
        }

        @JsonGetter("true-data-type")
        public String getTrueDataType() {
            return trueDataType;
        }

        @JsonSetter("true-data-type")
        public void setTrueDataType(String trueDataType) {
            this.trueDataType = trueDataType;
        }
    }
}
