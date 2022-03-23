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

package org.hkijena.jipipe.ui.cache;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeEmptyNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * Merges multiple {@link JIPipeDataTable}
 * Please not the previews are initialized with deferred rendering.
 * You will need to set a scroll pane to. Then the rendering will work.
 */
public class JIPipeExtendedMultiDataTableInfoModel implements TableModel {

    private final JTable table;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private final ArrayList<JIPipeProjectCompartment> compartmentList = new ArrayList<>();
    private final ArrayList<JIPipeGraphNode> nodeList = new ArrayList<>();
    private final List<String> textAnnotationColumns = new ArrayList<>();
    private final List<String> dataAnnotationColumns = new ArrayList<>();
    private final ArrayList<JIPipeDataTable> slotList = new ArrayList<>();
    private final ArrayList<Integer> rowList = new ArrayList<>();
    private final List<Component> previewCache = new ArrayList<>();
    private final Map<String, List<Component>> dataAnnotationPreviewCache = new HashMap<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();
    private JScrollPane scrollPane;
    private final boolean withCompartmentAndAlgorithm;

    public JIPipeExtendedMultiDataTableInfoModel(JTable table, boolean withCompartmentAndAlgorithm) {
        this.table = table;
        this.withCompartmentAndAlgorithm = withCompartmentAndAlgorithm;
    }

    public List<JIPipeDataTable> getSlotList() {
        return Collections.unmodifiableList(slotList);
    }

    /**
     * Adds an {@link JIPipeDataTableMetadata}
     *
     * @param project   The project
     * @param dataTable The data slot
     */
    public void add(JIPipeProject project, JIPipeDataTable dataTable) {
        for (String annotationColumn : dataTable.getAnnotationColumns()) {
            if (!textAnnotationColumns.contains(annotationColumn))
                textAnnotationColumns.add(annotationColumn);
        }
        for (String annotationColumn : dataTable.getDataAnnotationColumns()) {
            if (!dataAnnotationColumns.contains(annotationColumn))
                dataAnnotationColumns.add(annotationColumn);
        }
        JIPipeProjectCompartment compartment = null;
        if (project != null && dataTable instanceof JIPipeDataSlot) {
            compartment = project.getCompartments().getOrDefault(((JIPipeDataSlot) dataTable).getNode().getCompartmentUUIDInParentGraph(), null);
        }
        if (compartment == null) {
            compartment = new JIPipeProjectCompartment(new JIPipeEmptyNodeInfo());
        }
        JIPipeGraphNode node = null;
        if (dataTable instanceof JIPipeDataSlot) {
            node = ((JIPipeDataSlot) dataTable).getNode();
        }

        for (int i = 0; i < dataTable.getRowCount(); ++i) {
            slotList.add(dataTable);
            compartmentList.add(compartment);
            nodeList.add(node);
            rowList.add(i);
            previewCache.add(null);
        }
        for (String dataAnnotationColumn : dataTable.getDataAnnotationColumns()) {
            List<Component> dataAnnotationPreviews = dataAnnotationPreviewCache.getOrDefault(dataAnnotationColumn, null);
            if (dataAnnotationPreviews == null) {
                dataAnnotationPreviews = new ArrayList<>();
                dataAnnotationPreviewCache.put(dataAnnotationColumn, dataAnnotationPreviews);
            }
            for (int i = 0; i < dataTable.getRowCount(); ++i) {
                dataAnnotationPreviews.add(null);
            }
        }
    }

    private void revalidatePreviewCache() {
        if (dataSettings.getPreviewSize() != previewCacheSize) {
            for (int i = 0; i < previewCache.size(); i++) {
                previewCache.set(i, null);
            }
            for (List<Component> componentList : dataAnnotationPreviewCache.values()) {
                for (int i = 0; i < componentList.size(); i++) {
                    componentList.set(i, null);
                }
            }
            previewCacheSize = dataSettings.getPreviewSize();
        }
    }

    @Override
    public int getRowCount() {
        return rowList.size();
    }

    @Override
    public int getColumnCount() {
        if (withCompartmentAndAlgorithm)
            return textAnnotationColumns.size() + dataAnnotationColumns.size() + 7;
        else
            return textAnnotationColumns.size() + dataAnnotationColumns.size() + 5;
    }

    /**
     * Converts the column index to an annotation column index, or returns -1 if the column is not one
     *
     * @param columnIndex absolute column index
     * @return relative annotation column index, or -1
     */
    public int toAnnotationColumnIndex(int columnIndex) {
        if (withCompartmentAndAlgorithm) {
            if (columnIndex >= getDataAnnotationColumns().size() + 7)
                return columnIndex - getDataAnnotationColumns().size() - 7;
            else
                return -1;
        } else {
            if (columnIndex >= getDataAnnotationColumns().size() + 5)
                return columnIndex - getDataAnnotationColumns().size() - 5;
            else
                return -1;
        }
    }

    /**
     * Converts the column index to a data annotation column index, or returns -1 if the column is not one
     *
     * @param columnIndex absolute column index
     * @return relative data annotation column index, or -1
     */
    public int toDataAnnotationColumnIndex(int columnIndex) {
        if (withCompartmentAndAlgorithm) {
            if (columnIndex < getDataAnnotationColumns().size() + 7 && (columnIndex - 7) < getDataAnnotationColumns().size()) {
                return columnIndex - 7;
            } else {
                return -1;
            }
        } else {
            if (columnIndex < getDataAnnotationColumns().size() + 5 && (columnIndex - 5) < getDataAnnotationColumns().size()) {
                return columnIndex - 5;
            } else {
                return -1;
            }
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (withCompartmentAndAlgorithm) {
            if (columnIndex == 0)
                return "Slot";
            if (columnIndex == 1)
                return "Compartment";
            else if (columnIndex == 2)
                return "Algorithm";
            if (columnIndex == 3)
                return "Index";
            else if (columnIndex == 4)
                return "Data type";
            else if (columnIndex == 5)
                return "Preview";
            else if (columnIndex == 6)
                return "String representation";
            else if (toDataAnnotationColumnIndex(columnIndex) != -1)
                return dataAnnotationColumns.get(toDataAnnotationColumnIndex(columnIndex));
            else
                return textAnnotationColumns.get(toAnnotationColumnIndex(columnIndex));
        } else {
            if (columnIndex == 0)
                return "Slot";
            if (columnIndex == 1)
                return "Index";
            else if (columnIndex == 2)
                return "Data type";
            else if (columnIndex == 3)
                return "Preview";
            else if (columnIndex == 4)
                return "String representation";
            else if (toDataAnnotationColumnIndex(columnIndex) != -1)
                return dataAnnotationColumns.get(toDataAnnotationColumnIndex(columnIndex));
            else
                return textAnnotationColumns.get(toAnnotationColumnIndex(columnIndex));
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (withCompartmentAndAlgorithm) {
            if (columnIndex == 0)
                return String.class;
            else if (columnIndex == 1)
                return JIPipeProjectCompartment.class;
            else if (columnIndex == 2)
                return JIPipeGraphNode.class;
            else if (columnIndex == 3)
                return Integer.class;
            else if (columnIndex == 4)
                return JIPipeDataInfo.class;
            else if (columnIndex == 5)
                return Component.class;
            else if (columnIndex == 6)
                return String.class;
            else if (toDataAnnotationColumnIndex(columnIndex) != -1)
                return Component.class;
            else
                return JIPipeTextAnnotation.class;
        } else {
            if (columnIndex == 0)
                return String.class;
            else if (columnIndex == 1)
                return Integer.class;
            else if (columnIndex == 2)
                return JIPipeDataInfo.class;
            else if (columnIndex == 3)
                return Component.class;
            else if (columnIndex == 4)
                return String.class;
            else if (toDataAnnotationColumnIndex(columnIndex) != -1)
                return Component.class;
            else
                return JIPipeTextAnnotation.class;
        }
    }

    public boolean isWithCompartmentAndAlgorithm() {
        return withCompartmentAndAlgorithm;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (withCompartmentAndAlgorithm) {
            if (columnIndex == 0)
                return slotList.get(rowIndex).getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
            else if (columnIndex == 1)
                return compartmentList.get(rowIndex);
            else if (columnIndex == 2)
                return nodeList.get(rowIndex);
            else if (columnIndex == 3)
                return rowList.get(rowIndex);
            else if (columnIndex == 4)
                return JIPipeDataInfo.getInstance(slotList.get(rowIndex).getDataClass(rowList.get(rowIndex)));
            else if (columnIndex == 5) {
                return getMainDataPreviewComponent(rowIndex);
            } else if (columnIndex == 6)
                return "" + slotList.get(rowIndex).getVirtualData(rowList.get(rowIndex)).getStringRepresentation();
            else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
                return getDataAnnotationPreviewComponent(rowIndex, columnIndex);
            } else {
                String annotationColumn = textAnnotationColumns.get(toAnnotationColumnIndex(columnIndex));
                JIPipeDataTable slot = slotList.get(rowIndex);
                return slot.getTextAnnotationOr(rowList.get(rowIndex), annotationColumn, null);
            }
        } else {
            if (columnIndex == 0)
                return slotList.get(rowIndex).getLocation(JIPipeDataSlot.LOCATION_KEY_SLOT_NAME, "");
            else if (columnIndex == 1)
                return rowList.get(rowIndex);
            else if (columnIndex == 2)
                return JIPipeDataInfo.getInstance(slotList.get(rowIndex).getDataClass(rowList.get(rowIndex)));
            else if (columnIndex == 3) {
                return getMainDataPreviewComponent(rowIndex);
            } else if (columnIndex == 4)
                return "" + slotList.get(rowIndex).getVirtualData(rowList.get(rowIndex)).getStringRepresentation();
            else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
                return getDataAnnotationPreviewComponent(rowIndex, columnIndex);
            } else {
                String annotationColumn = textAnnotationColumns.get(toAnnotationColumnIndex(columnIndex));
                JIPipeDataTable slot = slotList.get(rowIndex);
                return slot.getTextAnnotationOr(rowList.get(rowIndex), annotationColumn, null);
            }
        }
    }

    private Component getDataAnnotationPreviewComponent(int rowIndex, int columnIndex) {
        revalidatePreviewCache();
        String dataAnnotationName = dataAnnotationColumns.get(toDataAnnotationColumnIndex(columnIndex));
        Component preview = dataAnnotationPreviewCache.get(dataAnnotationName).get(rowIndex);
        if (preview == null) {
            JIPipeDataAnnotation dataAnnotation = slotList.get(rowIndex).getDataAnnotation(rowList.get(rowIndex), dataAnnotationName);
            if (dataAnnotation != null && GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                preview = new JIPipeCachedDataPreview(table, dataAnnotation.getVirtualData(), true);
                dataAnnotationPreviewCache.get(dataAnnotationName).set(rowIndex, preview);
            } else {
                preview = new JLabel("N/A");
                dataAnnotationPreviewCache.get(dataAnnotationName).set(rowIndex, preview);
            }
        }
        return preview;
    }

    private Object getMainDataPreviewComponent(int rowIndex) {
        revalidatePreviewCache();
        Component preview = previewCache.get(rowIndex);
        if (preview == null) {
            if (GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                preview = new JIPipeCachedDataPreview(table, slotList.get(rowIndex).getVirtualData(rowList.get(rowIndex)), true);
                previewCache.set(rowIndex, preview);
            } else {
                preview = new JLabel("N/A");
                previewCache.set(rowIndex, preview);
            }
        }
        return preview;
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
    public List<String> getTextAnnotationColumns() {
        return textAnnotationColumns;
    }

    public List<String> getDataAnnotationColumns() {
        return dataAnnotationColumns;
    }

    /**
     * Gets the slot that defined the specified row
     *
     * @param row Row index
     * @return The slot that defined the row
     */
    public JIPipeDataTable getSlot(int row) {
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

    public JScrollPane getScrollPane() {
        return scrollPane;
    }

    public void setScrollPane(JScrollPane scrollPane) {
        this.scrollPane = scrollPane;
        initializeDeferredPreviewRendering();
    }

    /**
     * Adds some listeners to the scroll pane so we can
     */
    private void initializeDeferredPreviewRendering() {
        this.scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            updateRenderedPreviews();
        });
        updateRenderedPreviews();
    }

    public void updateRenderedPreviews() {
        JViewport viewport = scrollPane.getViewport();
        for (int row = 0; row < previewCache.size(); row++) {
            Component component = previewCache.get(row);
            if (component instanceof JIPipeCachedDataPreview) {
                updateRenderedPreviewComponent(viewport, row, (JIPipeCachedDataPreview) component);
            }
        }
        for (List<Component> componentList : dataAnnotationPreviewCache.values()) {
            for (int row = 0; row < componentList.size(); row++) {
                Component component = componentList.get(row);
                if (component instanceof JIPipeCachedDataPreview) {
                    updateRenderedPreviewComponent(viewport, row, (JIPipeCachedDataPreview) component);
                }
            }
        }
    }

    private void updateRenderedPreviewComponent(JViewport viewport, int modelRow, JIPipeCachedDataPreview component) {
        if (component.isRenderedOrRendering())
            return;
        // We assume view column = 0
        try {
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                Rectangle rect = table.getCellRect(viewRow, 0, true);
                Point pt = viewport.getViewPosition();
                rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                boolean overlaps = new Rectangle(viewport.getExtentSize()).intersects(rect);
                if (overlaps) {
                    component.renderPreview();
                }
            }
        } catch (IndexOutOfBoundsException e) {
        }
    }
}
