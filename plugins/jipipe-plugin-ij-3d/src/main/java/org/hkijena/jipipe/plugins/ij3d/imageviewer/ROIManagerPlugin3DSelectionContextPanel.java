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

package org.hkijena.jipipe.plugins.ij3d.imageviewer;

import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.plugins.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;

import javax.swing.*;
import java.util.List;

public abstract class ROIManagerPlugin3DSelectionContextPanel extends JPanel {

    private final ROIManagerPlugin3D roiManagerPlugin;

    public ROIManagerPlugin3DSelectionContextPanel(ROIManagerPlugin3D roiManagerPlugin) {
        this.roiManagerPlugin = roiManagerPlugin;
    }

    public ROIManagerPlugin3D getRoiManagerPlugin() {
        return roiManagerPlugin;
    }

    public JIPipeImageViewer getViewerPanel() {
        return roiManagerPlugin.getViewerPanel();
    }

    public abstract void selectionUpdated(ROI3DListData allROI, List<ROI3D> selectedROI);
}
