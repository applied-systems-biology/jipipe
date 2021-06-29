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

import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps around a {@link JIPipeDataSlot} to display a "toString" column
 */
public class JIPipeDataSlotTableModel implements TableModel {

    private final JTable table;
    private final JIPipeDataSlot slot;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private List<Component> previewCache = new ArrayList<>();
    private Map<String, List<Component>> dataAnnotationPreviewCache = new HashMap<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();
    private JScrollPane scrollPane;

    /**
     * Creates a new instance
     *
     * @param table the table
     * @param slot  the wrapped slot
     */
    public JIPipeDataSlotTableModel(JTable table, JIPipeDataSlot slot) {
        this.table = table;
        this.slot = slot;
        for (int i = 0; i < slot.getRowCount(); i++) {
            previewCache.add(null);
        }
        for (String annotationColumn : slot.getDataAnnotationColumns()) {
            List<Component> componentList = new ArrayList<>();
            for (int i = 0; i < slot.getRowCount(); i++) {
                componentList.add(null);
            }
            dataAnnotationPreviewCache.put(annotationColumn, componentList);
        }
    }

    public JIPipeDataSlot getSlot() {
        return slot;
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
        if (columnIndex >= slot.getDataAnnotationColumns().size() + 4)
            return columnIndex - slot.getDataAnnotationColumns().size() - 4;
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
        if (columnIndex < slot.getDataAnnotationColumns().size() + 4 && (columnIndex - 4) < slot.getDataAnnotationColumns().size()) {
            return columnIndex - 4;
        } else {
            return -1;
        }
    }

    @Override
    public int getRowCount() {
        return slot.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return slot.getAnnotationColumns().size() + slot.getDataAnnotationColumns().size() + 4;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0)
            return "Index";
        else if (columnIndex == 1)
            return "Type";
        else if (columnIndex == 2)
            return "Preview";
        else if (columnIndex == 3)
            return "String representation";
        else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            return "$" + slot.getDataAnnotationColumns().get(toDataAnnotationColumnIndex(columnIndex));
        } else {
            return slot.getAnnotationColumns().get(toAnnotationColumnIndex(columnIndex));
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
            return JIPipeAnnotation.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0)
            return rowIndex;
        else if (columnIndex == 1)
            return JIPipeDataInfo.getInstance(slot.getDataClass(rowIndex));
        else if (columnIndex == 2) {
            revalidatePreviewCache();
            Component preview = previewCache.get(rowIndex);
            if (preview == null) {
                if (GeneralDataSettings.getInstance().isGenerateCachePreviews()) {
                    preview = new JIPipeCachedDataPreview(table, slot.getVirtualData(rowIndex), true);
                    previewCache.set(rowIndex, preview);
                } else {
                    preview = new JLabel("N/A");
                    previewCache.set(rowIndex, preview);
                }
            }
            return preview;
        } else if (columnIndex == 3)
            return "" + slot.getVirtualData(rowIndex).getStringRepresentation();
        else if (toDataAnnotationColumnIndex(columnIndex) != -1) {
            revalidatePreviewCache();
            String dataAnnotationName = slot.getDataAnnotationColumns().get(toDataAnnotationColumnIndex(columnIndex));
            Component preview = dataAnnotationPreviewCache.get(dataAnnotationName).get(rowIndex);
            if (preview == null) {
                JIPipeDataAnnotation dataAnnotation = slot.getDataAnnotation(rowIndex, dataAnnotationName);
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
            return slot.getAnnotation(rowIndex, toAnnotationColumnIndex(columnIndex));
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

    private void updateRenderedPreviewComponent(JViewport viewport, int row, JIPipeCachedDataPreview component) {
        if (component.isRenderedOrRendering())
            return;
        // We assume view column = 0
        Rectangle rect = table.getCellRect(row, 0, true);
        Point pt = viewport.getViewPosition();
        rect.setLocation(rect.x - pt.x, rect.y - pt.y);
        boolean overlaps = new Rectangle(viewport.getExtentSize()).intersects(rect);
        if (overlaps) {
            component.renderPreview();
        }
    }
}
