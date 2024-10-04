/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data.serialization;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Merges multiple {@link JIPipeDataTableInfo}
 */
public class JIPipeMergedDataTableInfo implements TableModel {

    private final ArrayList<JIPipeProjectCompartment> compartmentList = new ArrayList<>();
    private final ArrayList<JIPipeGraphNode> algorithmList = new ArrayList<>();
    private final ArrayList<JIPipeDataTableRowInfo> rowList = new ArrayList<>();
    private final List<String> annotationColumns = new ArrayList<>();
    private final List<String> dataAnnotationColumns = new ArrayList<>();
    private final ArrayList<JIPipeDataSlot> slotList = new ArrayList<>();
    private final List<JIPipeDataTableInfo> addedTables = new ArrayList<>();

    /**
     * Adds an {@link JIPipeDataTableInfo}
     *
     * @param project  The project
     * @param dataSlot The data slot
     * @param table    The table
     */
    public void add(JIPipeProject project, JIPipeDataSlot dataSlot, JIPipeDataTableInfo table) {
        addedTables.add(table);
        for (String annotationColumn : table.getTextAnnotationColumns()) {
            if (!annotationColumns.contains(annotationColumn))
                annotationColumns.add(annotationColumn);
        }
        for (String annotationColumn : table.getDataAnnotationColumns()) {
            if (!dataAnnotationColumns.contains(annotationColumn))
                dataAnnotationColumns.add(annotationColumn);
        }
        UUID compartmentUUID = dataSlot.getNode().getCompartmentUUIDInParentGraph();
        JIPipeProjectCompartment compartment = project.getCompartments().get(compartmentUUID);
        JIPipeGraphNode algorithm = dataSlot.getNode();

        for (JIPipeDataTableRowInfo row : table.getRowList()) {
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
        return annotationColumns.size() + dataAnnotationColumns.size() + 5;
    }

    /**
     * Converts the column index to an annotation column index, or returns -1 if the column is not one
     *
     * @param columnIndex absolute column index
     * @return relative annotation column index, or -1
     */
    public int toAnnotationColumnIndex(int columnIndex) {
        if (columnIndex >= getDataAnnotationColumns().size() + 5)
            return columnIndex - getDataAnnotationColumns().size() - 5;
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
        if (columnIndex < getDataAnnotationColumns().size() + 5 && (columnIndex - 5) < getDataAnnotationColumns().size()) {
            return columnIndex - 5;
        } else {
            return -1;
        }
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
        else if (toDataAnnotationColumnIndex(columnIndex) != -1)
            return "$" + dataAnnotationColumns.get(toDataAnnotationColumnIndex(columnIndex));
        else
            return annotationColumns.get(toAnnotationColumnIndex(columnIndex));
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
            return JIPipeDataInfo.class;
        else if (columnIndex == 4)
            return JIPipeDataTableRowInfo.class;
        else if (toDataAnnotationColumnIndex(columnIndex) != -1)
            return JIPipeDataAnnotationInfo.class;
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
            return compartmentList.get(rowIndex);
        else if (columnIndex == 1)
            return algorithmList.get(rowIndex);
        else if (columnIndex == 2)
            return rowList.get(rowIndex).getIndex();
        else if (columnIndex == 3)
            return JIPipeDataInfo.getInstance(slotList.get(rowIndex).getAcceptedDataType());
        else if (columnIndex == 4)
            return rowList.get(rowIndex);
        else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            String annotationColumn = dataAnnotationColumns.get(toDataAnnotationColumnIndex(columnIndex));
            return rowList.get(rowIndex).getDataAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
        } else {
            String annotationColumn = annotationColumns.get(toAnnotationColumnIndex(columnIndex));
            return rowList.get(rowIndex).getTextAnnotations().stream().filter(t -> t.nameEquals(annotationColumn)).findFirst().orElse(null);
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

    public List<String> getDataAnnotationColumns() {
        return dataAnnotationColumns;
    }

    /**
     * @return Additional columns
     */
    public List<String> getAnnotationColumns() {
        return annotationColumns;
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
    public List<JIPipeDataTableRowInfo> getRowList() {
        return rowList;
    }

    public List<JIPipeDataTableInfo> getAddedTables() {
        return Collections.unmodifiableList(addedTables);
    }
}
