package org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager;

import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel2D;

import javax.swing.*;
import java.util.List;

public abstract class ROIManagerPluginSelectionContextPanel extends JPanel {

    private final ROIManagerPlugin2D roiManagerPlugin;

    public ROIManagerPluginSelectionContextPanel(ROIManagerPlugin2D roiManagerPlugin) {
        this.roiManagerPlugin = roiManagerPlugin;
    }

    public ROIManagerPlugin2D getRoiManagerPlugin() {
        return roiManagerPlugin;
    }

    public ImageViewerPanel2D getViewerPanel() {
        return roiManagerPlugin.getViewerPanel();
    }

    public abstract void selectionUpdated(ROIListData allROI, List<Roi> selectedROI);
}
