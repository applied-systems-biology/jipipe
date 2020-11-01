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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeEmptyNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.components.JIPipeCachedDataPreview;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges multiple {@link JIPipeDataSlot}
 */
public class JIPipeMergedDataSlotTable implements TableModel {

    private final JTable table;
    private ArrayList<JIPipeProjectCompartment> compartmentList = new ArrayList<>();
    private ArrayList<JIPipeGraphNode> algorithmList = new ArrayList<>();
    private List<String> traitColumns = new ArrayList<>();
    private ArrayList<JIPipeDataSlot> slotList = new ArrayList<>();
    private ArrayList<Integer> rowList = new ArrayList<>();
    private List<Component> previewCache = new ArrayList<>();

    public JIPipeMergedDataSlotTable(JTable table) {
        this.table = table;
    }

    /**
     * Adds an {@link JIPipeExportedDataTable}
     *
     * @param project  The project
     * @param dataSlot The data slot
     */
    public void add(JIPipeProject project, JIPipeDataSlot dataSlot) {
        for (String traitColumn : dataSlot.getAnnotationColumns()) {
            if (!traitColumns.contains(traitColumn))
                traitColumns.add(traitColumn);
        }
        JIPipeProjectCompartment compartment = project.getCompartments().getOrDefault(dataSlot.getNode().getCompartment(), null);
        if (compartment == null) {
            compartment = new JIPipeProjectCompartment(new JIPipeEmptyNodeInfo());
            compartment.setCustomName(dataSlot.getNode().getCompartment());
        }
        JIPipeGraphNode algorithm = dataSlot.getNode();

        for (int i = 0; i < dataSlot.getRowCount(); ++i) {
            slotList.add(dataSlot);
            compartmentList.add(compartment);
            algorithmList.add(algorithm);
            rowList.add(i);
            previewCache.add(null);
        }
    }

    @Override
    public int getRowCount() {
        return rowList.size();
    }

    @Override
    public int getColumnCount() {
        return traitColumns.size() + 6;
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
            return "Preview";
        else if (columnIndex == 5)
            return "String representation";
        else
            return traitColumns.get(columnIndex - 6);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return JIPipeProjectCompartment.class;
        else if (columnIndex == 1)
            return JIPipeGraphNode.class;
        else if (columnIndex == 2)
            return Integer.class;
        else if (columnIndex == 3)
            return JIPipeData.class;
        else if (columnIndex == 4)
            return Component.class;
        else if (columnIndex == 5)
            return String.class;
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
            return rowList.get(rowIndex);
        else if (columnIndex == 3)
            return slotList.get(rowIndex).getData(rowList.get(rowIndex), JIPipeData.class);
        else if (columnIndex == 4) {
            Component preview = previewCache.get(rowIndex);
            if (preview == null) {
                if (GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                    JIPipeData data = slotList.get(rowIndex).getData(rowList.get(rowIndex), JIPipeData.class);
                    preview = new JIPipeCachedDataPreview(table, data);
                    previewCache.set(rowIndex, preview);
                } else {
                    preview = new JLabel("N/A");
                    previewCache.set(rowIndex, preview);
                }
            }
            return preview;
        } else if (columnIndex == 5)
            return "" + slotList.get(rowIndex).getData(rowList.get(rowIndex), JIPipeData.class);
        else {
            String traitColumn = traitColumns.get(columnIndex - 6);
            JIPipeDataSlot slot = slotList.get(rowIndex);
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
