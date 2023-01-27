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
 *
 */

package org.hkijena.jipipe.ui.datatable;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataPreview;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A table model that displays data batches
 */
public class JIPipeSimpleDataBatchTableModel implements TableModel {

    private final List<JIPipeMergingDataBatch> dataBatchList;
    private final List<String> inputSlotNames = new ArrayList<>();
    private final List<String> annotationColumns = new ArrayList<>();
    private final Map<String, List<Component>> previews = new HashMap<>();
    private final List<Map<String, JIPipeDataItemStore>> previewedData = new ArrayList<>();
    private final JTable table;
    private JScrollPane scrollPane;

    public JIPipeSimpleDataBatchTableModel(JTable table, List<JIPipeMergingDataBatch> dataBatchList) {
        this.table = table;
        this.dataBatchList = dataBatchList;


        Set<String> inputSlotNameSet = new HashSet<>();
        Set<String> annotationColumnSet = new HashSet<>();
        for (JIPipeMergingDataBatch dataBatch : dataBatchList) {
            Map<String, JIPipeDataItemStore> previewMap = new HashMap<>();
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : dataBatch.getInputSlotRows().entrySet()) {
                inputSlotNameSet.add(entry.getKey().getName());
                // We just preview any data available
                if (entry.getValue().size() > 0) {
                    previewMap.put(entry.getKey().getName(), entry.getKey().getVirtualData(entry.getValue().iterator().next()));
                }
            }
            annotationColumnSet.addAll(dataBatch.getMergedTextAnnotations().keySet());
            previewedData.add(previewMap);
        }

        inputSlotNames.addAll(inputSlotNameSet);
        annotationColumns.addAll(annotationColumnSet);

        for (String name : inputSlotNameSet) {
            List<Component> previewsForSlot = new ArrayList<>();
            for (int i = 0; i < dataBatchList.size(); i++) {
                previewsForSlot.add(null);
            }
            previews.put(name, previewsForSlot);
        }
    }

    @Override
    public int getRowCount() {
        return dataBatchList.size();
    }

    @Override
    public int getColumnCount() {
        // Index + Previews + annotations
        return 1 + inputSlotNames.size() + annotationColumns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "Index";
        } else if (columnIndex - 1 < inputSlotNames.size()) {
            return inputSlotNames.get(columnIndex - 1);
        } else {
            return annotationColumns.get(columnIndex - 1 - inputSlotNames.size());
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Integer.class;
        } else if (columnIndex - 1 < inputSlotNames.size()) {
            return Component.class;
        } else {
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return rowIndex;
        } else if (columnIndex - 1 < inputSlotNames.size()) {
            String slotName = inputSlotNames.get(columnIndex - 1);
            Component preview = previews.get(slotName).get(rowIndex);
            if (preview == null) {
                JIPipeDataItemStore previewed = previewedData.get(rowIndex).getOrDefault(slotName, null);
                if (previewed != null) {
                    preview = new JIPipeCachedDataPreview(table, previewed, true);
                } else {
                    preview = new JLabel("N/A");
                }
                previews.get(slotName).set(rowIndex, preview);
            }
            return preview;
        } else {
            String column = annotationColumns.get(columnIndex - 1 - inputSlotNames.size());
            JIPipeTextAnnotation annotation = dataBatchList.get(rowIndex).getMergedTextAnnotations().getOrDefault(column, null);
            if (annotation != null)
                return annotation.getValue();
            else
                return null;
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
        for (List<Component> list : previews.values()) {
            for (int row = 0; row < list.size(); row++) {
                Component component = list.get(row);
                if (component instanceof JIPipeCachedDataPreview) {
                    if (((JIPipeCachedDataPreview) component).isRenderedOrRendering())
                        continue;
                    // We assume view column = 0
                    Rectangle rect = table.getCellRect(row, 0, true);
                    Point pt = viewport.getViewPosition();
                    rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                    boolean overlaps = new Rectangle(viewport.getExtentSize()).intersects(rect);
                    if (overlaps) {
                        ((JIPipeCachedDataPreview) component).renderPreview();
                    }
                }
            }
        }
    }
}
