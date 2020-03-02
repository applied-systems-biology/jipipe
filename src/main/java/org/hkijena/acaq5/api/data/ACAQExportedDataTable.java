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
    private Class<?> acceptedDataType;
    private List<Row> rowList;
    private List<ACAQTraitDeclaration> traitColumns;

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

    public ACAQExportedDataTable() {

    }

    @JsonGetter("algorithm-id")
    public String getAlgorithmId() {
        return algorithmId;
    }

    @JsonSetter("algorithm-id")
    private void setAlgorithmId(String algorithmId) {
        this.algorithmId = algorithmId;
    }

    @JsonGetter("slot")
    public String getSlotName() {
        return slotName;
    }

    @JsonSetter("slot")
    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    @JsonGetter("internal-path")
    public Path getInternalPath() {
        return internalPath;
    }

    @JsonSetter("internal-path")
    public void setInternalPath(Path internalPath) {
        this.internalPath = internalPath;
    }

    @JsonGetter("data-type")
    public String getAcceptedDataTypeClassName() {
        return acceptedDataType.getCanonicalName();
    }

    @JsonSetter("data-type")
    public void setAcceptedDataTypeClassName(String className) {
        this.acceptedDataType = ACAQDatatypeRegistry.getInstance().findDataClass(className);
    }

    @JsonGetter("rows")
    public List<Row> getRowList() {
        return rowList;
    }

    @JsonSetter("rows")
    public void setRowList(List<Row> rowList) {
        this.rowList = rowList;
    }

    public void saveAsJson(Path fileName) throws IOException {
        JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(fileName.toFile(), this);
    }

    public void saveAsCSV(Path fileName) throws IOException {
        ResultsTable table = new ResultsTable();
        for (Row row : rowList) {
            table.incrementCounter();
            table.addValue("acaq:algorithm-id", algorithmId);
            table.addValue("acaq:slot", slotName);
            table.addValue("acaq:data-type", acceptedDataType.getCanonicalName());
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

    public static ACAQExportedDataTable loadFromJson(Path fileName) {
        try {
            return JsonUtils.getObjectMapper().readerFor(ACAQExportedDataTable.class).readValue(fileName.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Row {
        private Path location;
        private List<ACAQTrait> traits;

        public Row() {
        }

        @JsonGetter("location")
        public Path getLocation() {
            return location;
        }

        @JsonSetter("location")
        public void setLocation(Path location) {
            this.location = location;
        }

        @JsonGetter("traits")
        public List<ACAQTrait> getTraits() {
            return traits;
        }

        @JsonSetter("traits")
        public void setTraits(List<ACAQTrait> traits) {
            this.traits = traits;
        }
    }
}
