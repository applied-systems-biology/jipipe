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
