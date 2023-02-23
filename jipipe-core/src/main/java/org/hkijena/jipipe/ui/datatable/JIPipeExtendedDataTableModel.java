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

import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataPreview;
import org.hkijena.jipipe.utils.data.Store;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps around a {@link org.hkijena.jipipe.api.data.JIPipeDataTable} to display additional columns.
 * For example, it is capable of displaying previews.
 */
public class JIPipeExtendedDataTableModel implements TableModel {

    private final JTable table;
    private final Store<JIPipeDataTable> dataTableStore;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private final List<Component> previewCache = new ArrayList<>();
    private final Map<String, List<Component>> dataAnnotationPreviewCache = new HashMap<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();
    private JScrollPane scrollPane;

    /**
     * Creates a new instance
     *
     * @param table              the table
     * @param dataTableStore the wrapped slot
     */
    public JIPipeExtendedDataTableModel(JTable table, Store<JIPipeDataTable> dataTableStore) {
        this.table = table;
        this.dataTableStore = dataTableStore;

        JIPipeDataTable dataTable = dataTableStore.get();
        if (dataTable != null) {
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                previewCache.add(null);
            }
            for (String annotationColumn : dataTable.getDataAnnotationColumns()) {
                List<Component> componentList = new ArrayList<>();
                for (int i = 0; i < dataTable.getRowCount(); i++) {
                    componentList.add(null);
                }
                dataAnnotationPreviewCache.put(annotationColumn, componentList);
            }
        }
    }

    public JIPipeDataTable getDataTable() {
        return dataTableStore.get();
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
        JIPipeDataTable dataTable = dataTableStore.get();
        if(dataTable != null) {
            if (columnIndex >= dataTable.getDataAnnotationColumns().size() + 4)
                return columnIndex - dataTable.getDataAnnotationColumns().size() - 4;
            else
                return -1;
        }
        else {
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
        JIPipeDataTable dataTable = dataTableStore.get();
        if(dataTable != null) {
            if (columnIndex < dataTable.getDataAnnotationColumns().size() + 4 && (columnIndex - 4) < dataTable.getDataAnnotationColumns().size()) {
                return columnIndex - 4;
            } else {
                return -1;
            }
        }
        else {
            return -1;
        }
    }

    @Override
    public int getRowCount() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if(dataTable != null) {
            return dataTable.getRowCount();
        }
        else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        JIPipeDataTable dataTable = dataTableStore.get();
        if(dataTable != null) {
            return dataTable.getTextAnnotationColumns().size() + dataTable.getDataAnnotationColumns().size() + 4;
        }
        else {
            return 4;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        JIPipeDataTable dataTable = dataTableStore.get();
        if (columnIndex == 0)
            return "Index";
        else if (columnIndex == 1)
            return "Type";
        else if (columnIndex == 2)
            return "Preview";
        else if (columnIndex == 3)
            return "String representation";
        else if (toDataAnnotationColumnIndex(columnIndex) != -1 && dataTable != null) {
            return "$" + dataTable.getDataAnnotationColumns().get(toDataAnnotationColumnIndex(columnIndex));
        } else if(dataTable != null) {
            return dataTable.getTextAnnotationColumns().get(toAnnotationColumnIndex(columnIndex));
        }
        else {
            return "NA";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0)
            return Integer.class;
        else if (columnIndex == 1)
            return JIPipeDataInfo.class;
        else if (columnIndex == 2)
            return Component.class;
        else if (columnIndex == 3)
            return String.class;
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
        JIPipeDataTable dataTable = dataTableStore.get();
        if(dataTable != null) {
            if (columnIndex == 0) {
                return rowIndex;
            } else if (columnIndex == 1) {
                try {
                    return JIPipeDataInfo.getInstance(dataTable.getDataClass(rowIndex));
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            } else if (columnIndex == 2) {
                revalidatePreviewCache();
                Component preview = previewCache.get(rowIndex);
                if (preview == null) {
                    if (GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                        preview = new JIPipeCachedDataPreview(table, dataTable.getDataItemStore(rowIndex), true);
                        previewCache.set(rowIndex, preview);
                    } else {
                        preview = new JLabel("N/A");
                        previewCache.set(rowIndex, preview);
                    }
                }
                return preview;
            } else if (columnIndex == 3)
                try {
                    return "" + dataTable.getDataItemStore(rowIndex).getStringRepresentation();
                } catch (IndexOutOfBoundsException e) {
                    return "<Invalid>";
                }
            else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
                revalidatePreviewCache();
                String dataAnnotationName = dataTable.getDataAnnotationColumns().get(toDataAnnotationColumnIndex(columnIndex));
                Component preview = dataAnnotationPreviewCache.get(dataAnnotationName).get(rowIndex);
                if (preview == null) {
                    JIPipeDataAnnotation dataAnnotation = dataTable.getDataAnnotation(rowIndex, dataAnnotationName);
                    if (dataAnnotation != null && GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                        preview = new JIPipeCachedDataPreview(table, dataAnnotation.getVirtualData(), true);
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
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            }
        }
        else {
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
        try {
            // We assume view column = 0
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
