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

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges multiple {@link JIPipeExportedDataTable}
 */
public class JIPipeMergedExportedDataTable implements TableModel {

    private ArrayList<JIPipeProjectCompartment> compartmentList = new ArrayList<>();
    private ArrayList<JIPipeGraphNode> algorithmList = new ArrayList<>();
    private ArrayList<JIPipeExportedDataTable.Row> rowList = new ArrayList<>();
    private List<String> traitColumns = new ArrayList<>();
    private ArrayList<JIPipeDataSlot> slotList = new ArrayList<>();

    /**
     * Adds an {@link JIPipeExportedDataTable}
     *
     * @param project  The project
     * @param dataSlot The data slot
     * @param table    The table
     */
    public void add(JIPipeProject project, JIPipeDataSlot dataSlot, JIPipeExportedDataTable table) {
        for (String traitColumn : table.getTraitColumns()) {
            if (!traitColumns.contains(traitColumn))
                traitColumns.add(traitColumn);
        }
        String compartmentName = dataSlot.getNode().getCompartment();
        JIPipeProjectCompartment compartment = project.getCompartments().get(compartmentName);
        JIPipeGraphNode algorithm = dataSlot.getNode();

        for (JIPipeExportedDataTable.Row row : table.getRowList()) {
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
            return traitColumns.get(columnIndex - 4);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return JIPipeProjectCompartment.class;
        else if (columnIndex == 1)
            return JIPipeGraphNode.class;
        if (columnIndex == 2)
            return Path.class;
        else if (columnIndex == 3)
            return JIPipeExportedDataTable.Row.class;
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
            return compartmentList.get(rowIndex);
        else if (columnIndex == 1)
            return algorithmList.get(rowIndex);
        else if (columnIndex == 2)
            return rowList.get(rowIndex).getLocation();
        else if (columnIndex == 3)
            return rowList.get(rowIndex);
        else {
            String traitColumn = traitColumns.get(columnIndex - 4);
            return rowList.get(rowIndex).getTraits().stream().filter(t -> t.nameEquals(traitColumn)).findFirst().orElse(null);
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
     */
    public List<String> getTraitColumns() {
        return traitColumns;
    }

    /**
     * Gets the slot that defined the specified row
     *
     * @param row Row index
     * @return The slot that defined the row
     */
    public JIPipeDataSlot getSlot(int row) {
        return slotList.get(row);
    }

    /**
     * @return List of rows
     */
    public List<JIPipeExportedDataTable.Row> getRowList() {
        return rowList;
    }
}
