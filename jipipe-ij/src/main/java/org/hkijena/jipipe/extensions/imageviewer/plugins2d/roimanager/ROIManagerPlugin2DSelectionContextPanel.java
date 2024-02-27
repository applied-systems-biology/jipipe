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

package org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager;

import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;

import javax.swing.*;
import java.util.List;

public abstract class ROIManagerPlugin2DSelectionContextPanel extends JPanel {

    private final ROIManagerPlugin2D roiManagerPlugin;

    public ROIManagerPlugin2DSelectionContextPanel(ROIManagerPlugin2D roiManagerPlugin) {
        this.roiManagerPlugin = roiManagerPlugin;
    }

    public ROIManagerPlugin2D getRoiManagerPlugin() {
        return roiManagerPlugin;
    }

    public JIPipeImageViewer getViewerPanel() {
        return roiManagerPlugin.getViewerPanel();
    }

    public abstract void selectionUpdated(ROIListData allROI, List<Roi> selectedROI);
}
