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

package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class AddROIToActiveJIPipeImageViewerDataDisplayOperation implements JIPipeDesktopDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        JIPipeImageViewer viewerPanel = JIPipeImageViewer.getActiveViewerPanel();
        if (viewerPanel == null) {
            JOptionPane.showMessageDialog(desktopWorkbench.getWindow(), "There is no active JIPipe image viewer.", "Add to active image viewer", JOptionPane.ERROR_MESSAGE);
        } else {
            viewerPanel.addOverlay(data);
        }
    }

    @Override
    public String getId() {
        return "jipipe:add-roi-to-active-jipipe-image-viewer";
    }

    @Override
    public String getName() {
        return "Add to active image viewer";
    }

    @Override
    public String getDescription() {
        return "Adds the ROI to the (last) active JIPipe image viewer.";
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }
}
