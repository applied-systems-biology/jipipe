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
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultDataSlotPreview;
import org.hkijena.jipipe.plugins.settings.JIPipeGeneralDataApplicationSettings;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders row data of an {@link JIPipeDataTableMetadata}
 */
public class JIPipeDesktopRowDataTableCellRenderer implements TableCellRenderer {

    private final JTable table;
    private final JScrollPane scrollPane;
    private final JIPipeGeneralDataApplicationSettings dataSettings = JIPipeGeneralDataApplicationSettings.getInstance();
    private JIPipeDesktopProjectWorkbench workbenchUI;
    private JIPipeDataSlot slot;
    private List<JIPipeDesktopResultDataSlotPreview> previewCache = new ArrayList<>();
    private int previewCacheSize = JIPipeGeneralDataApplicationSettings.getInstance().getPreviewSize();

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param table       the table
     * @param scrollPane  thr scroll pane
     */
    public JIPipeDesktopRowDataTableCellRenderer(JIPipeDesktopProjectWorkbench workbenchUI, JIPipeDataSlot slot, JTable table, JScrollPane scrollPane) {
        this.workbenchUI = workbenchUI;
        this.slot = slot;
        this.table = table;
        this.scrollPane = scrollPane;
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
            row = table.convertRowIndexToModel(row);
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
