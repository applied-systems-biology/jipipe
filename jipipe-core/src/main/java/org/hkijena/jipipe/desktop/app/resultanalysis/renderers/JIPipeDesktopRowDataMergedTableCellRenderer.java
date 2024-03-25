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

package org.hkijena.jipipe.desktop.app.resultanalysis.renderers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.api.data.serialization.JIPipeMergedDataTableMetadata;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultDataSlotPreview;
import org.hkijena.jipipe.plugins.settings.GeneralDataSettings;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders data in {@link JIPipeMergedDataTableMetadata}
 */
public class JIPipeDesktopRowDataMergedTableCellRenderer implements TableCellRenderer {

    private final JIPipeMergedDataTableMetadata mergedDataTable;
    private final JScrollPane scrollPane;
    private final JTable table;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private JIPipeDesktopProjectWorkbench workbenchUI;
    private List<JIPipeDesktopResultDataSlotPreview> previewCache = new ArrayList<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();

    /**
     * @param workbenchUI     The workbench
     * @param mergedDataTable the table to be displayed
     * @param scrollPane      the scroll pane
     * @param table           the table
     */
    public JIPipeDesktopRowDataMergedTableCellRenderer(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeMergedDataTableMetadata mergedDataTable, JScrollPane scrollPane, JTable table) {
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
        if (value instanceof JIPipeDataTableMetadataRow) {
            JIPipeMergedDataTableMetadata model = (JIPipeMergedDataTableMetadata) table.getModel();
            JIPipeDataSlot slot = model.getSlot(table.convertRowIndexToModel(row));
            while (row > previewCache.size() - 1) {
                previewCache.add(null);
            }
            revalidatePreviewCache();
            JIPipeDesktopResultDataSlotPreview preview = previewCache.get(row);
            if (preview == null) {
                preview = JIPipe.getDataTypes().getCellRendererFor(workbenchUI, table, slot, (JIPipeDataTableMetadataRow) value, null);
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
        for (int modelRow = 0; modelRow < previewCache.size(); modelRow++) {
            JIPipeDesktopResultDataSlotPreview component = previewCache.get(modelRow);
            // We assume view column = 0
            try {
                int viewRow = table.convertRowIndexToView(modelRow);
                if (viewRow >= 0) {
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
}
