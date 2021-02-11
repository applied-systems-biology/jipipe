package org.hkijena.jipipe.ui.batchassistant;

import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeVirtualData;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataPreview;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A table model that displays data batches
 */
public class DataBatchTableModel implements TableModel {

    private final List<JIPipeMergingDataBatch> dataBatchList;
    private final List<String> inputSlotNames = new ArrayList<>();
    private final List<String> annotationColumns = new ArrayList<>();
    private final List<Component> previews = new ArrayList<>();
    private final List<Map<String, JIPipeVirtualData>> previewedData = new ArrayList<>();
    private final JTable table;
    private JScrollPane scrollPane;

    public DataBatchTableModel(JTable table, List<JIPipeMergingDataBatch> dataBatchList) {
        this.table = table;
        this.dataBatchList = dataBatchList;


        Set<String> inputSlotNameSet = new HashSet<>();
        Set<String> annotationColumnSet = new HashSet<>();
        for (JIPipeMergingDataBatch dataBatch : dataBatchList) {
            Map<String, JIPipeVirtualData> previewMap = new HashMap<>();
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : dataBatch.getInputSlotRows().entrySet()) {
                inputSlotNameSet.add(entry.getKey().getName());
                // We just preview any data available
                if (entry.getValue().size() > 0) {
                    previewMap.put(entry.getKey().getName(), entry.getKey().getVirtualData(entry.getValue().iterator().next()));
                }
            }
            annotationColumnSet.addAll(dataBatch.getAnnotations().keySet());
            previewedData.add(previewMap);
        }

        inputSlotNames.addAll(inputSlotNameSet);
        annotationColumns.addAll(annotationColumnSet);

        for (int i = 0; i < dataBatchList.size(); i++) {
            previews.add(null);
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

    @Nls
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
            Component preview = previews.get(rowIndex);
            if (preview == null) {
                String slotName = inputSlotNames.get(columnIndex - 1);
                JIPipeVirtualData previewed = previewedData.get(rowIndex).getOrDefault(slotName, null);
                if (previewed != null) {
                    preview = new JIPipeCachedDataPreview(table, previewed, true);
                } else {
                    preview = new JLabel("N/A");
                }
                previews.set(rowIndex, preview);
            }
            return preview;
        } else {
            String column = annotationColumns.get(columnIndex - 1 - inputSlotNames.size());
            JIPipeAnnotation annotation = dataBatchList.get(rowIndex).getAnnotations().getOrDefault(column, null);
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
        for (int row = 0; row < previews.size(); row++) {
            Component component = previews.get(row);
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
