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
import org.hkijena.jipipe.ui.cache.JIPipeCachedDataPreview;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Merges multiple {@link JIPipeDataSlot}
 * Please not the previews are initialized with deferred rendering.
 * You will need to set a scroll pane to. Then the rendering will work.
 */
public class JIPipeMergedDataSlotTable implements TableModel {

    private final JTable table;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private ArrayList<JIPipeProjectCompartment> compartmentList = new ArrayList<>();
    private ArrayList<JIPipeGraphNode> algorithmList = new ArrayList<>();
    private List<String> traitColumns = new ArrayList<>();
    private ArrayList<JIPipeDataSlot> slotList = new ArrayList<>();
    private ArrayList<Integer> rowList = new ArrayList<>();
    private List<Component> previewCache = new ArrayList<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();
    private JScrollPane scrollPane;
    private boolean withCompartmentAndAlgorithm;

    public JIPipeMergedDataSlotTable(JTable table, boolean withCompartmentAndAlgorithm) {
        this.table = table;
        this.withCompartmentAndAlgorithm = withCompartmentAndAlgorithm;
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

    private void revalidatePreviewCache() {
        if (dataSettings.getPreviewSize() != previewCacheSize) {
            for (int i = 0; i < previewCache.size(); i++) {
                previewCache.set(i, null);
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
        if(withCompartmentAndAlgorithm)
            return traitColumns.size() + 6;
        else
            return traitColumns.size() + 4;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if(withCompartmentAndAlgorithm) {
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
        else {
            if (columnIndex == 0)
                return "Index";
            else if (columnIndex == 1)
                return "Data type";
            else if (columnIndex == 2)
                return "Preview";
            else if (columnIndex == 3)
                return "String representation";
            else
                return traitColumns.get(columnIndex - 4);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if(withCompartmentAndAlgorithm) {
            if (columnIndex == 0)
                return JIPipeProjectCompartment.class;
            else if (columnIndex == 1)
                return JIPipeGraphNode.class;
            else if (columnIndex == 2)
                return Integer.class;
            else if (columnIndex == 3)
                return JIPipeDataInfo.class;
            else if (columnIndex == 4)
                return Component.class;
            else if (columnIndex == 5)
                return String.class;
            else
                return JIPipeAnnotation.class;
        }
        else {
            if (columnIndex == 0)
                return Integer.class;
            else if (columnIndex == 1)
                return JIPipeDataInfo.class;
            else if (columnIndex == 2)
                return Component.class;
            else if (columnIndex == 3)
                return String.class;
            else
                return JIPipeAnnotation.class;
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
        if(withCompartmentAndAlgorithm) {
            if (columnIndex == 0)
                return compartmentList.get(rowIndex);
            else if (columnIndex == 1)
                return algorithmList.get(rowIndex);
            else if (columnIndex == 2)
                return rowList.get(rowIndex);
            else if (columnIndex == 3)
                return JIPipeDataInfo.getInstance(slotList.get(rowIndex).getDataClass(rowList.get(rowIndex)));
            else if (columnIndex == 4) {
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
            } else if (columnIndex == 5)
                return "" + slotList.get(rowIndex).getVirtualData(rowList.get(rowIndex)).getStringRepresentation();
            else {
                String traitColumn = traitColumns.get(columnIndex - 6);
                JIPipeDataSlot slot = slotList.get(rowIndex);
                return slot.getAnnotationOr(rowList.get(rowIndex), traitColumn, null);
            }
        }
        else {
            if (columnIndex == 0)
                return rowList.get(rowIndex);
            else if (columnIndex == 1)
                return JIPipeDataInfo.getInstance(slotList.get(rowIndex).getDataClass(rowList.get(rowIndex)));
            else if (columnIndex == 2) {
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
            } else if (columnIndex == 3)
                return "" + slotList.get(rowIndex).getVirtualData(rowList.get(rowIndex)).getStringRepresentation();
            else {
                String traitColumn = traitColumns.get(columnIndex - 4);
                JIPipeDataSlot slot = slotList.get(rowIndex);
                return slot.getAnnotationOr(rowList.get(rowIndex), traitColumn, null);
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
