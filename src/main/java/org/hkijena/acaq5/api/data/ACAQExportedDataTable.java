package org.hkijena.acaq5.api.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import ij.measure.ResultsTable;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.utils.JsonUtils;

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
 * Contains all metadata exported from an {@link ACAQDataSlot}
 */
public class ACAQExportedDataTable implements TableModel {
    private String algorithmId;
    private String slotName;
    private Path internalPath;
    private Class<? extends ACAQData> acceptedDataType;
    private List<Row> rowList;
    private List<ACAQTraitDeclaration> traitColumns;

    /**
     * Initializes a new table from a slot
     * @param slot The slot
     * @param dataOutputPaths output path for each slot row
     */
    public ACAQExportedDataTable(ACAQDataSlot slot, List<Path> dataOutputPaths) {
        this.algorithmId = slot.getAlgorithm().getDeclaration().getId();
        this.slotName = slot.getName();
        this.internalPath = slot.getStoragePath();
        this.acceptedDataType = slot.getAcceptedDataType();
        this.rowList = new ArrayList<>();
        for (int row = 0; row < slot.getRowCount(); ++row) {
            Row rowInstance = new Row();
            rowInstance.location = dataOutputPaths.get(row);
            rowInstance.traits = slot.getAnnotations(row);
            rowList.add(rowInstance);
        }
    }

    /**
     * Creates an empty instance
     */
    public ACAQExportedDataTable() {

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
    public Path getInternalPath() {
        return internalPath;
    }

    /**
     * Sets the internal path
     * @param internalPath The internal path
     */
    @JsonSetter("internal-path")
    public void setInternalPath(Path internalPath) {
        this.internalPath = internalPath;
    }

    /**
     * @return The accepted datatype ID
     */
    @JsonGetter("data-type")
    public String getAcceptedDataTypeId() {
        return ACAQDatatypeRegistry.getInstance().getIdOf(acceptedDataType);
    }

    /**
     * Sets the accepted datatype ID
     * @param id Datatype ID
     */
    @JsonSetter("data-type")
    public void setAcceptedDataTypeId(String id) {
        this.acceptedDataType = ACAQDatatypeRegistry.getInstance().getById(id);
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
     * @param rowList Row list
     */
    @JsonSetter("rows")
    public void setRowList(List<Row> rowList) {
        this.rowList = rowList;
    }

    /**
     * Saves the table to JSON
     * @param fileName  JSON file
     * @throws IOException Triggered by {@link com.fasterxml.jackson.databind.ObjectMapper}
     */
    public void saveAsJson(Path fileName) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    /**
     * Saves the table to CSV
     * @param fileName  CSV file
     * @throws IOException Triggered by {@link ResultsTable}
     */
    public void saveAsCSV(Path fileName) throws IOException {
        ResultsTable table = new ResultsTable();
        for (Row row : rowList) {
            table.incrementCounter();
            table.addValue("acaq:algorithm-id", algorithmId);
            table.addValue("acaq:slot", slotName);
            table.addValue("acaq:data-type", ACAQDatatypeRegistry.getInstance().getIdOf(acceptedDataType));
            table.addValue("acaq:internal-path", internalPath.toString());
            table.addValue("acaq:location", row.location.toString());
            for (ACAQTraitDeclaration traitColumn : getTraitColumns()) {
                ACAQTrait existing = row.traits.stream().filter(t -> t.getDeclaration() == traitColumn).findFirst().orElse(null);
                if (existing instanceof ACAQDiscriminator)
                    table.addValue(traitColumn.getName(), ((ACAQDiscriminator) existing).getValue());
                else if (existing != null)
                    table.addValue(traitColumn.getName(), 1);
                else
                    table.addValue(traitColumn.getName(), 0);
            }
        }
        table.saveAs(fileName.toString());
    }

    /**
     * @return Additional columns containing {@link ACAQTraitDeclaration}
     */
    public List<ACAQTraitDeclaration> getTraitColumns() {
        if (traitColumns == null) {
            Set<ACAQTraitDeclaration> registeredTraits = new HashSet<>();
            for (Row row : rowList) {
                registeredTraits.addAll(row.traits.stream().map(ACAQTrait::getDeclaration).collect(Collectors.toSet()));
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
        return getTraitColumns().size() + 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Location";
        else if (columnIndex == 1)
            return "Data";
        else
            return traitColumns.get(columnIndex - 2).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return Path.class;
        else if (columnIndex == 1)
            return Row.class;
        else
            return ACAQTrait.class;
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
            return rowList.get(rowIndex);
        else {
            ACAQTraitDeclaration traitColumn = traitColumns.get(columnIndex - 2);
            return rowList.get(rowIndex).traits.stream().filter(t -> t.getDeclaration() == traitColumn).findFirst().orElse(null);
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
     * @param fileName JSON file
     * @return Loaded table
     */
    public static ACAQExportedDataTable loadFromJson(Path fileName) {
        try {
            return JsonUtils.getObjectMapper().readerFor(ACAQExportedDataTable.class).readValue(fileName.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A row in the table
     */
    public static class Row {
        private Path location;
        private List<ACAQTrait> traits;

        /**
         * Creates new instance
         */
        public Row() {
        }

        /**
         * @return Internal location relative to the output folder
         */
        @JsonGetter("location")
        public Path getLocation() {
            return location;
        }

        /**
         * Sets the location
         * @param location Internal location relative to the output folder
         */
        @JsonSetter("location")
        public void setLocation(Path location) {
            this.location = location;
        }

        /**
         * @return Annotations
         */
        @JsonGetter("traits")
        public List<ACAQTrait> getTraits() {
            return traits;
        }

        /**
         * Sets annotations
         * @param traits List of annotations
         */
        @JsonSetter("traits")
        public void setTraits(List<ACAQTrait> traits) {
            this.traits = traits;
        }
    }
}
