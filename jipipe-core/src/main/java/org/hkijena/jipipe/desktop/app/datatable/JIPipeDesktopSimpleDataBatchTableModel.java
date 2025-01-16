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

package org.hkijena.jipipe.desktop.app.datatable;

import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.desktop.app.cache.JIPipeDesktopCachedDataPreview;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.data.Store;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * A table model that displays iteration steps
 */
public class JIPipeDesktopSimpleDataBatchTableModel implements TableModel {

    private final List<JIPipeMultiIterationStep> iterationStepList;
    private final List<String> inputSlotNames = new ArrayList<>();
    private final List<String> annotationColumns = new ArrayList<>();
    private final Map<String, List<Component>> previews = new HashMap<>();
    private final List<Map<String, Store<JIPipeDataItemStore>>> previewedData = new ArrayList<>();
    private final JTable table;
    private JScrollPane scrollPane;

    public JIPipeDesktopSimpleDataBatchTableModel(JTable table, List<JIPipeMultiIterationStep> iterationStepList, Class<? extends Store> storeClass) {
        this.table = table;
        this.iterationStepList = iterationStepList;


        Set<String> inputSlotNameSet = new HashSet<>();
        Set<String> annotationColumnSet = new HashSet<>();
        for (JIPipeMultiIterationStep iterationStep : iterationStepList) {
            Map<String, Store<JIPipeDataItemStore>> previewMap = new HashMap<>();
            for (Map.Entry<JIPipeDataSlot, Set<Integer>> entry : iterationStep.getInputSlotRows().entrySet()) {
                inputSlotNameSet.add(entry.getKey().getName());
                // We just preview any data available
                if (entry.getValue().size() > 0) {
                    previewMap.put(entry.getKey().getName(),
                            (Store<JIPipeDataItemStore>) ReflectionUtils.newInstance(storeClass,
                                    entry.getKey().getDataItemStore(entry.getValue().iterator().next())));
                }
            }
            annotationColumnSet.addAll(iterationStep.getMergedTextAnnotations().keySet());
            previewedData.add(previewMap);
        }

        inputSlotNames.addAll(inputSlotNameSet);
        annotationColumns.addAll(annotationColumnSet);

        for (String name : inputSlotNameSet) {
            List<Component> previewsForSlot = new ArrayList<>();
            for (int i = 0; i < iterationStepList.size(); i++) {
                previewsForSlot.add(null);
            }
            previews.put(name, previewsForSlot);
        }
    }

    @Override
    public int getRowCount() {
        return iterationStepList.size();
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
                Store<JIPipeDataItemStore> previewed = previewedData.get(rowIndex).getOrDefault(slotName, null);
                if (previewed != null && previewed.isPresent()) {
                    preview = new JIPipeDesktopCachedDataPreview(table, previewed, true);
                } else {
                    preview = new JLabel("N/A");
                }
                previews.get(slotName).set(rowIndex, preview);
            }
            return preview;
        } else {
            String column = annotationColumns.get(columnIndex - 1 - inputSlotNames.size());
            JIPipeTextAnnotation annotation = iterationStepList.get(rowIndex).getMergedTextAnnotations().getOrDefault(column, null);
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
                if (component instanceof JIPipeDesktopCachedDataPreview) {
                    if (((JIPipeDesktopCachedDataPreview) component).isRenderedOrRendering())
                        continue;
                    // We assume view column = 0
                    Rectangle rect = table.getCellRect(row, 0, true);
                    Point pt = viewport.getViewPosition();
                    rect.setLocation(rect.x - pt.x, rect.y - pt.y);
                    boolean overlaps = new Rectangle(viewport.getExtentSize()).intersects(rect);
                    if (overlaps) {
                        ((JIPipeDesktopCachedDataPreview) component).renderPreview();
                    }
                }
            }
        }
    }
}
