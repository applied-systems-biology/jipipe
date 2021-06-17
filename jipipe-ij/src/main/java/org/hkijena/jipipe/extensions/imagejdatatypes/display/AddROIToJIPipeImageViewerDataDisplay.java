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

package org.hkijena.jipipe.extensions.imagejdatatypes.display;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeResultSlotDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerWindow;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ROIManagerPlugin;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.WindowListCellRenderer;
import org.hkijena.jipipe.utils.ListSelectionMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AddROIToJIPipeImageViewerDataDisplay implements JIPipeDataDisplayOperation, JIPipeDataImportOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        List<ImageViewerWindow> windows = new ArrayList<>(ImageViewerWindow.getOpenWindows());
        if (windows.isEmpty()) {
            JOptionPane.showMessageDialog(workbench.getWindow(), "There are no active JIPipe image viewers.", "Add to image viewer", JOptionPane.ERROR_MESSAGE);
            return;
        }
        windows.sort(Comparator.comparing(Frame::getTitle));
        if (ImageViewerWindow.getActiveWindow() != null) {
            windows.remove(ImageViewerWindow.getActiveWindow());
            windows.add(0, ImageViewerWindow.getActiveWindow());
        }
        List<ImageViewerWindow> selected = UIUtils.getSelectionByDialog(workbench.getWindow(),
                windows,
                Collections.singleton(windows.get(0)),
                "Add to image viewer",
                "Please select one or multiple image viewers.",
                new WindowListCellRenderer<>(),
                ListSelectionMode.MultipleInterval);
        for (ImageViewerWindow window : selected) {
            for (ImageViewerPanelPlugin plugin : window.getViewerPanel().getPlugins().stream()
                    .filter(plugin -> plugin instanceof ROIManagerPlugin).collect(Collectors.toList())) {
                ((ROIManagerPlugin) plugin).importROIs((ROIListData) data);
            }
        }
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        ROIListData rois = ROIListData.importFrom(rowStorageFolder);
        display(rois, displayName, workbench, new JIPipeResultSlotDataSource(slot, row, rowStorageFolder));
        return rois;
    }

    @Override
    public String getName() {
        return "Add to image viewer";
    }

    @Override
    public String getDescription() {
        return "Adds the ROI to one or multiple JIPipe image viewers.";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }
}
