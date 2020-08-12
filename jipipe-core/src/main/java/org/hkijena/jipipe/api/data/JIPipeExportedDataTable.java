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
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.registries.JIPipeDatatypeRegistry;
import org.hkijena.jipipe.utils.JsonUtils;
import org.python.antlr.ast.Str;

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
 */
public class JIPipeExportedDataTable implements TableModel {
    private String algorithmId;
    private String slotName;
    private String internalPath;
    private Class<? extends JIPipeData> acceptedDataType;
    private List<Row> rowList;
    private List<String> traitColumns;

    /**
     * Initializes a new table from a slot
     *
     * @param slot            The slot
     * @param dataOutputPaths output path for each slot row
     */
    public JIPipeExportedDataTable(JIPipeDataSlot slot, Path basePath, List<Path> dataOutputPaths) {
        this.algorithmId = slot.getNode().getInfo().getId();
        this.slotName = slot.getName();
        if(basePath != null) {
            this.internalPath = basePath.relativize(slot.getStoragePath()).toString();
        }
        else {
            this.internalPath = slot.getStoragePath().toString();
        }
        this.acceptedDataType = slot.getAcceptedDataType();
        this.rowList = new ArrayList<>();
        for (int row = 0; row < slot.getRowCount(); ++row) {
            Row rowInstance = new Row();
            rowInstance.location = dataOutputPaths.get(row).toString();
            rowInstance.traits = slot.getAnnotations(row);
            rowList.add(rowInstance);
        }
    }

    public JIPipeExportedDataTable() {
    }

    /**
     * @return Gets the algorithm ID
     */
    @JsonGetter("algorithm-id")
    public String getAlgorithmId() {
        return algorithmId;
    }

    /**
     * Sets the algorithm ID
     *
     * @param algorithmId the algorithm ID
     */
    @JsonSetter("algorithm-id")
    private void setAlgorithmId(String algorithmId) {
        this.algorithmId = algorithmId;
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
        return JIPipeDatatypeRegistry.getInstance().getIdOf(acceptedDataType);
    }

    /**
     * Sets the accepted datatype ID
     *
     * @param id Datatype ID
     */
    @JsonSetter("data-type")
    public void setAcceptedDataTypeId(String id) {
        this.acceptedDataType = JIPipeDatatypeRegistry.getInstance().getById(id);
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
     * Saves the table to CSV
     *
     * @param fileName CSV file
     * @throws IOException Triggered by {@link ResultsTable}
     */
    public void saveAsCSV(Path fileName) throws IOException {
        ResultsTable table = new ResultsTable();
        for (Row row : rowList) {
            table.incrementCounter();
            table.addValue("jipipe:algorithm-id", algorithmId);
            table.addValue("jipipe:slot", slotName);
            table.addValue("jipipe:data-type", JIPipeDatatypeRegistry.getInstance().getIdOf(acceptedDataType));
            table.addValue("jipipe:internal-path", internalPath.toString());
            table.addValue("jipipe:location", row.location.toString());
            for (String traitColumn : getTraitColumns()) {
                JIPipeAnnotation existing = row.traits.stream().filter(t -> t.nameEquals(traitColumn)).findFirst().orElse(null);
                if (existing != null)
                    table.addValue(traitColumn, existing.getValue());
                else
                    table.addValue(traitColumn, "");
            }
        }
        table.saveAs(fileName.toString());
    }

    /**
     * @return Additional columns
     */
    public List<String> getTraitColumns() {
        if (traitColumns == null) {
            Set<String> registeredTraits = new HashSet<>();
            for (Row row : rowList) {
                registeredTraits.addAll(row.traits.stream().map(JIPipeAnnotation::getName).collect(Collectors.toSet()));
            }
            traitColumns = new ArrayList<>(registeredTraits);
        }
        return traitColumns;
    }

    @Override
    public int getRowCount() {
        return rowList.size();
    }

    @Override
    public int getColumnCount() {
        return getTraitColumns().size() + 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Location";
        else if (columnIndex == 1)
            return "Data type";
        else if(columnIndex == 2) {
            return "Preview";
        }
        else
            return traitColumns.get(columnIndex - 3);
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
            return rowList.get(rowIndex).getLocation();
        else if (columnIndex == 1)
            return JIPipeDataInfo.getInstance(acceptedDataType);
        else if (columnIndex == 2)
            return rowList.get(rowIndex);
        else {
            String traitColumn = traitColumns.get(columnIndex - 3);
            return rowList.get(rowIndex).traits.stream().filter(t -> t.nameEquals(traitColumn)).findFirst().orElse(null);
        }
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
        private String location;
        private List<JIPipeAnnotation> traits;

        /**
         * Creates new instance
         */
        public Row() {
        }

        /**
         * @return Internal location relative to the output folder
         */
        @JsonGetter("location")
        public String getLocation() {
            return location;
        }

        /**
         * Sets the location
         *
         * @param location Internal location relative to the output folder
         */
        @JsonSetter("location")
        public void setLocation(String location) {
            this.location = location;
        }

        /**
         * @return Annotations
         */
        @JsonGetter("traits")
        public List<JIPipeAnnotation> getTraits() {
            return traits;
        }

        /**
         * Sets annotations
         *
         * @param traits List of annotations
         */
        @JsonSetter("traits")
        public void setTraits(List<JIPipeAnnotation> traits) {
            this.traits = traits;
        }
    }
}
