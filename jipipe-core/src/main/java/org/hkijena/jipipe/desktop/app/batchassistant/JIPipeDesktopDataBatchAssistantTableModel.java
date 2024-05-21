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

package org.hkijena.jipipe.desktop.app.batchassistant;

import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCachedDataPreview;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps around a {@link JIPipeDataTable} to display additional columns.
 * For example, it is capable of displaying previews.
 */
public class JIPipeDesktopDataBatchAssistantTableModel implements TableModel {

    private final JTable table;
    private final JIPipeDataTable dataTable;
    private final JIPipeGeneralDataApplicationSettings dataSettings = JIPipeGeneralDataApplicationSettings.getInstance();
    private final List<Component> previewCache = new ArrayList<>();
    private final Map<String, List<Component>> dataAnnotationPreviewCache = new HashMap<>();
    private int previewCacheSize = JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize();
    private JScrollPane scrollPane;

    /**
     * Creates a new instance
     *
     * @param table     the table
     * @param dataTable the wrapped slot
     */
    public JIPipeDesktopDataBatchAssistantTableModel(JTable table, JIPipeDataTable dataTable) {
        this.table = table;
        this.dataTable = dataTable;
        for (int i = 0; i < dataTable.getRowCount(); i++) {
            previewCache.add(null);
        }
        for (String annotationColumn : dataTable.getDataAnnotationColumnNames()) {
            List<Component> componentList = new ArrayList<>();
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                componentList.add(null);
            }
            dataAnnotationPreviewCache.put(annotationColumn, componentList);
        }
    }

    public JIPipeDataTable getDataTable() {
        return dataTable;
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

    /**
     * Converts the column index to an annotation column index, or returns -1 if the column is not one
     *
     * @param columnIndex absolute column index
     * @return relative annotation column index, or -1
     */
    public int toAnnotationColumnIndex(int columnIndex) {
        if (columnIndex >= dataTable.getDataAnnotationColumnNames().size() + 2)
            return columnIndex - dataTable.getDataAnnotationColumnNames().size() - 2;
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
        if (columnIndex < dataTable.getDataAnnotationColumnNames().size() + 2 && (columnIndex - 2) < dataTable.getDataAnnotationColumnNames().size()) {
            return columnIndex - 2;
        } else {
            return -1;
        }
    }

    @Override
    public int getRowCount() {
        return dataTable.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return dataTable.getTextAnnotationColumnNames().size() + dataTable.getDataAnnotationColumnNames().size() + 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Index";
        else if (columnIndex == 1)
            return "Status";
        else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            return "Slot: " + dataTable.getDataAnnotationColumnNames().get(toDataAnnotationColumnIndex(columnIndex));
        } else {
            return dataTable.getTextAnnotationColumnNames().get(toAnnotationColumnIndex(columnIndex));
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return Integer.class;
        else if (columnIndex == 1)
            return Component.class;
        else if (toDataAnnotationColumnIndex(columnIndex) != -1)
            return Component.class;
        else {
            return JIPipeTextAnnotation.class;
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
        } else if (columnIndex == 1) {
            revalidatePreviewCache();
            Component preview = previewCache.get(rowIndex);
            if (preview == null) {
                if (JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews()) {
                    preview = new JIPipeDesktopCachedDataPreview(table, dataTable.getDataItemStore(rowIndex), true);
                    previewCache.set(rowIndex, preview);
                } else {
                    preview = new JLabel("N/A");
                    previewCache.set(rowIndex, preview);
                }
            }
            return preview;
        } else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            revalidatePreviewCache();
            String dataAnnotationName = dataTable.getDataAnnotationColumnNames().get(toDataAnnotationColumnIndex(columnIndex));
            Component preview = dataAnnotationPreviewCache.get(dataAnnotationName).get(rowIndex);
            if (preview == null) {
                JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(rowIndex, dataAnnotationName);
                if (dataAnnotation != null && JIPipeGeneralDataApplicationSettings.getInstance().isGenerateCachePreviews()) {
                    preview = new JIPipeDesktopCachedDataPreview(table, dataAnnotation.getDataItemStore(), true);
                    dataAnnotationPreviewCache.get(dataAnnotationName).set(rowIndex, preview);
                } else {
                    preview = new JLabel("N/A");
                    dataAnnotationPreviewCache.get(dataAnnotationName).set(rowIndex, preview);
                }
            }
            return preview;
        } else {
            try {
                return dataTable.getTextAnnotation(rowIndex, toAnnotationColumnIndex(columnIndex));
            } catch (IndexOutOfBoundsException ex) {
                return null;
            }
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
        if (scrollPane == null)
            return;
        JViewport viewport = scrollPane.getViewport();
        for (int row = 0; row < previewCache.size(); row++) {
            Component component = previewCache.get(row);
            if (component instanceof JIPipeDesktopCachedDataPreview) {
                updateRenderedPreviewComponent(viewport, row, (JIPipeDesktopCachedDataPreview) component);
            }
        }
        for (List<Component> componentList : dataAnnotationPreviewCache.values()) {
            for (int row = 0; row < componentList.size(); row++) {
                Component component = componentList.get(row);
                if (component instanceof JIPipeDesktopCachedDataPreview) {
                    updateRenderedPreviewComponent(viewport, row, (JIPipeDesktopCachedDataPreview) component);
                }
            }
        }
    }

    private void updateRenderedPreviewComponent(JViewport viewport, int modelRow, JIPipeDesktopCachedDataPreview component) {
        if (component.isRenderedOrRendering())
            return;
        try {
            int viewRow = table.convertRowIndexToView(modelRow);
            if (viewRow >= 0) {
                // We assume view column = 0
                Rectangle rect = table.getCellRect(modelRow, 0, true);
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
