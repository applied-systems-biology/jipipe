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
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel2D;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelPlugin2D;
import org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager2d.ROIManagerPlugin2D;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.renderers.ComponentListCellRenderer;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.ListSelectionMode;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AddROIToJIPipeImageViewerDataDisplay implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        List<ImageViewerPanel2D> viewerPanels = new ArrayList<>(ImageViewerPanel2D.getOpenViewerPanels());
        if (viewerPanels.isEmpty()) {
            JOptionPane.showMessageDialog(workbench.getWindow(), "There are no active JIPipe image viewers.", "Add to image viewer", JOptionPane.ERROR_MESSAGE);
            return;
        }
        viewerPanels.sort(Comparator.comparing(Component::getName));
        if (ImageViewerPanel2D.getActiveViewerPanel() != null) {
            viewerPanels.remove(ImageViewerPanel2D.getActiveViewerPanel());
            viewerPanels.add(0, ImageViewerPanel2D.getActiveViewerPanel());
        }
        List<ImageViewerPanel2D> selected = UIUtils.getSelectionByDialog(workbench.getWindow(),
                viewerPanels,
                Collections.singleton(viewerPanels.get(0)),
                "Add to image viewer",
                "Please select one or multiple image viewers.",
                new ComponentListCellRenderer<>(UIUtils.getIconFromResources("actions/window.png")),
                ListSelectionMode.MultipleInterval);
        for (ImageViewerPanel2D viewerPanel : selected) {
            for (ImageViewerPanelPlugin2D plugin : viewerPanel.getPlugins().stream()
                    .filter(plugin -> plugin instanceof ROIManagerPlugin2D).collect(Collectors.toList())) {
                ((ROIManagerPlugin2D) plugin).importROIs((ROIListData) data, false);
            }
        }
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

    @Override
    public String getId() {
        return "jipipe:add-roi-to-jipipe-image-viewer";
    }
}
