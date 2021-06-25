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

package org.hkijena.jipipe.ui.resultanalysis;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTableRow;
import org.hkijena.jipipe.api.data.JIPipeMergedExportedDataTable;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders data in {@link JIPipeMergedExportedDataTable}
 */
public class JIPipeRowDataMergedTableCellRenderer implements TableCellRenderer {

    private final JIPipeMergedExportedDataTable mergedDataTable;
    private final JScrollPane scrollPane;
    private final JTable table;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private JIPipeProjectWorkbench workbenchUI;
    private List<JIPipeResultDataSlotPreview> previewCache = new ArrayList<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();

    /**
     * @param workbenchUI     The workbench
     * @param mergedDataTable the table to be displayed
     * @param scrollPane      the scroll pane
     * @param table           the table
     */
    public JIPipeRowDataMergedTableCellRenderer(JIPipeProjectWorkbench workbenchUI, JIPipeMergedExportedDataTable mergedDataTable, JScrollPane scrollPane, JTable table) {
        this.workbenchUI = workbenchUI;
        this.mergedDataTable = mergedDataTable;
        this.scrollPane = scrollPane;
        this.table = table;
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> updateRenderedPreviews());
        updateRenderedPreviews();
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
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JIPipeExportedDataTableRow) {
            JIPipeMergedExportedDataTable model = (JIPipeMergedExportedDataTable) table.getModel();
            JIPipeDataSlot slot = model.getSlot(table.convertRowIndexToModel(row));
            while (row > previewCache.size() - 1) {
                previewCache.add(null);
            }
            revalidatePreviewCache();
            JIPipeResultDataSlotPreview preview = previewCache.get(row);
            if (preview == null) {
                preview = JIPipe.getDataTypes().getCellRendererFor(workbenchUI, table, slot, (JIPipeExportedDataTableRow) value);
                previewCache.set(row, preview);
            }
            if (isSelected) {
                preview.setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                preview.setBackground(UIManager.getColor("List.background"));
            }
            return preview;
        }
        return null;
    }

    public void updateRenderedPreviews() {
        JViewport viewport = scrollPane.getViewport();
        for (int row = 0; row < previewCache.size(); row++) {
            JIPipeResultDataSlotPreview component = previewCache.get(row);
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
}
