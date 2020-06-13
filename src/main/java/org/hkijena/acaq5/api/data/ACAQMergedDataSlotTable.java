package org.hkijena.acaq5.api.data;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.algorithm.ACAQEmptyAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges multiple {@link ACAQDataSlot}
 */
public class ACAQMergedDataSlotTable implements TableModel {

    private ArrayList<ACAQProjectCompartment> compartmentList = new ArrayList<>();
    private ArrayList<ACAQGraphNode> algorithmList = new ArrayList<>();
    private List<String> traitColumns = new ArrayList<>();
    private ArrayList<ACAQDataSlot> slotList = new ArrayList<>();
    private ArrayList<Integer> rowList = new ArrayList<>();

    /**
     * Adds an {@link ACAQExportedDataTable}
     *
     * @param project  The project
     * @param dataSlot The data slot
     */
    public void add(ACAQProject project, ACAQDataSlot dataSlot) {
        for (String traitColumn : dataSlot.getAnnotationColumns()) {
            if (!traitColumns.contains(traitColumn))
                traitColumns.add(traitColumn);
        }
        ACAQProjectCompartment compartment = project.getCompartments().getOrDefault(dataSlot.getAlgorithm().getCompartment(), null);
        if (compartment == null) {
            compartment = new ACAQProjectCompartment(new ACAQEmptyAlgorithmDeclaration());
            compartment.setCustomName(dataSlot.getAlgorithm().getCompartment());
        }
        ACAQGraphNode algorithm = dataSlot.getAlgorithm();

        for (int i = 0; i < dataSlot.getRowCount(); ++i) {
            slotList.add(dataSlot);
            compartmentList.add(compartment);
            algorithmList.add(algorithm);
            rowList.add(i);
        }
    }

    @Override
    public int getRowCount() {
        return rowList.size();
    }

    @Override
    public int getColumnCount() {
        return traitColumns.size() + 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Compartment";
        else if (columnIndex == 1)
            return "Algorithm";
        if (columnIndex == 2)
            return "Index";
        else if (columnIndex == 3)
            return "Data type";
        else if (columnIndex == 4)
            return "String representation";
        else
            return traitColumns.get(columnIndex - 5);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return ACAQProjectCompartment.class;
        else if (columnIndex == 1)
            return ACAQGraphNode.class;
        if (columnIndex == 2)
            return Integer.class;
        else if (columnIndex == 3)
            return ACAQData.class;
        else if (columnIndex == 4)
            return String.class;
        else
            return ACAQAnnotation.class;
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
            return rowList.get(rowIndex);
        else if (columnIndex == 3)
            return slotList.get(rowIndex).getData(rowList.get(rowIndex), ACAQData.class);
        else if (columnIndex == 4)
            return "" + slotList.get(rowIndex).getData(rowList.get(rowIndex), ACAQData.class);
        else {
            String traitColumn = traitColumns.get(columnIndex - 5);
            ACAQDataSlot slot = slotList.get(rowIndex);
            return slot.getAnnotationOr(rowList.get(rowIndex), traitColumn, null);
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
     * @return Additional columns
     * */
    public List<String> getTraitColumns() {
        return traitColumns;
    }

    /**
     * Gets the slot that defined the specified row
     *
     * @param row Row index
     * @return The slot that defined the row
     */
    public ACAQDataSlot getSlot(int row) {
        return slotList.get(row);
    }

    /**
     * @return List of rows
     */
    public List<Integer> getRowList() {
        return rowList;
    }

    /**
     * Returns the location
     *
     * @param multiRow the row
     * @return the row at the slot at the row
     */
    public int getRow(int multiRow) {
        return rowList.get(multiRow);
    }
}
