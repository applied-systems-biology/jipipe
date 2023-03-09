package org.hkijena.jipipe.extensions.ij3d.imageviewer;

import ij.gui.Roi;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;

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
