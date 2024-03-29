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

package org.hkijena.jipipe.ui.resultanalysis.renderers;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadata;
import org.hkijena.jipipe.api.data.JIPipeExportedDataAnnotation;
import org.hkijena.jipipe.extensions.settings.GeneralDataSettings;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotPreview;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders row data of an {@link JIPipeDataTableMetadata}
 */
public class JIPipeRowDataAnnotationTableCellRenderer implements TableCellRenderer {

    private final JTable table;
    private final JScrollPane scrollPane;
    private final GeneralDataSettings dataSettings = GeneralDataSettings.getInstance();
    private JIPipeProjectWorkbench workbenchUI;
    private JIPipeDataSlot slot;
    private Map<String, List<JIPipeResultDataSlotPreview>> previewCache = new HashMap<>();
    private int previewCacheSize = GeneralDataSettings.getInstance().getPreviewSize();

    /**
     * @param workbenchUI the workbench
     * @param slot        the data slot
     * @param table       the table
     * @param scrollPane  thr scroll pane
     */
    public JIPipeRowDataAnnotationTableCellRenderer(JIPipeProjectWorkbench workbenchUI, JIPipeDataSlot slot, JTable table, JScrollPane scrollPane) {
        this.workbenchUI = workbenchUI;
        this.slot = slot;
        this.table = table;
        this.scrollPane = scrollPane;
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> updateRenderedPreviews());
        updateRenderedPreviews();
    }

    private void revalidatePreviewCache() {
        if (dataSettings.getPreviewSize() != previewCacheSize) {
            for (List<JIPipeResultDataSlotPreview> previews : previewCache.values()) {
                for (int i = 0; i < previews.size(); i++) {
                    previews.set(i, null);
                }
            }
            previewCacheSize = dataSettings.getPreviewSize();
        }
    }

    private JIPipeResultDataSlotPreview getPreviewComponent(int row, JIPipeExportedDataAnnotation annotation) {
        List<JIPipeResultDataSlotPreview> previews = previewCache.getOrDefault(annotation.getName(), null);
        if (previews == null) {
            previews = new ArrayList<>();
            previewCache.put(annotation.getName(), previews);
        }
        while (row > previews.size() - 1) {
            previews.add(null);
        }
        return previews.get(row);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (value instanceof JIPipeExportedDataAnnotation) {
            JIPipeExportedDataAnnotation exportedDataAnnotation = (JIPipeExportedDataAnnotation) value;

            row = table.convertRowIndexToModel(row);
            revalidatePreviewCache();
            JIPipeResultDataSlotPreview preview = getPreviewComponent(row, exportedDataAnnotation);

            if (preview == null) {
                preview = JIPipe.getDataTypes().getCellRendererFor(workbenchUI, table, slot, exportedDataAnnotation.getTableRow(), exportedDataAnnotation);
                previewCache.get(exportedDataAnnotation.getName()).set(row, preview);
            }
            if (isSelected) {
                preview.setBackground(UIManager.getColor("List.selectionBackground"));
            } else {
                preview.setBackground(UIManager.getColor("List.background"));
            }
            return preview;
        }
        JLabel label = new JLabel("NA");
        label.setForeground(Color.RED);
        return label;
    }

    public void updateRenderedPreviews() {
        JViewport viewport = scrollPane.getViewport();
        for (List<JIPipeResultDataSlotPreview> previews : previewCache.values()) {
            for (int modelRow = 0; modelRow < previews.size(); modelRow++) {
                JIPipeResultDataSlotPreview component = previews.get(modelRow);
                if (component == null)
                    continue;
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
    }
}
