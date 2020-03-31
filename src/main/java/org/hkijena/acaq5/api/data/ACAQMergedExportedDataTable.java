package org.hkijena.acaq5.api.data;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges multiple {@link ACAQExportedDataTable}
 */
public class ACAQMergedExportedDataTable implements TableModel {

    private ArrayList<ACAQProjectCompartment> compartmentList = new ArrayList<>();
    private ArrayList<ACAQAlgorithm> algorithmList = new ArrayList<>();
    private ArrayList<ACAQExportedDataTable.Row> rowList = new ArrayList<>();
    private List<ACAQTraitDeclaration> traitColumns = new ArrayList<>();
    private ArrayList<ACAQDataSlot> slotList = new ArrayList<>();

    /**
     * Adds an {@link ACAQExportedDataTable}
     * @param project The project
     * @param dataSlot The data slot
     * @param table The table
     */
    public void add(ACAQProject project, ACAQDataSlot dataSlot, ACAQExportedDataTable table) {
        for (ACAQTraitDeclaration traitColumn : table.getTraitColumns()) {
            if (!traitColumns.contains(traitColumn))
                traitColumns.add(traitColumn);
        }
        String compartmentName = dataSlot.getAlgorithm().getCompartment();
        ACAQProjectCompartment compartment = project.getCompartments().get(compartmentName);
        ACAQAlgorithm algorithm = dataSlot.getAlgorithm();

        for (ACAQExportedDataTable.Row row : table.getRowList()) {
            slotList.add(dataSlot);
            compartmentList.add(compartment);
            algorithmList.add(algorithm);
            rowList.add(row);
        }
    }

    @Override
    public int getRowCount() {
        return rowList.size();
    }

    @Override
    public int getColumnCount() {
        return traitColumns.size() + 4;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Compartment";
        else if (columnIndex == 1)
            return "Algorithm";
        if (columnIndex == 2)
            return "Location";
        else if (columnIndex == 3)
            return "Data";
        else
            return traitColumns.get(columnIndex - 4).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return ACAQProjectCompartment.class;
        else if (columnIndex == 1)
            return ACAQAlgorithm.class;
        if (columnIndex == 2)
            return Path.class;
        else if (columnIndex == 3)
            return ACAQExportedDataTable.Row.class;
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
            return compartmentList.get(rowIndex);
        else if (columnIndex == 1)
            return algorithmList.get(rowIndex);
        else if (columnIndex == 2)
            return rowList.get(rowIndex).getLocation();
        else if (columnIndex == 3)
            return rowList.get(rowIndex);
        else {
            ACAQTraitDeclaration traitColumn = traitColumns.get(columnIndex - 4);
            return rowList.get(rowIndex).getTraits().stream().filter(t -> t.getDeclaration() == traitColumn).findFirst().orElse(null);
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
     * @return Additional columns containing {@link ACAQTraitDeclaration}
     */
    public List<ACAQTraitDeclaration> getTraitColumns() {
        return traitColumns;
    }

    /**
     * Gets the slot that defined the specified row
     * @param row Row index
     * @return The slot that defined the row
     */
    public ACAQDataSlot getSlot(int row) {
        return slotList.get(row);
    }

    /**
     * @return List of rows
     */
    public List<ACAQExportedDataTable.Row> getRowList() {
        return rowList;
    }
}
