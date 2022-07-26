package org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager;

import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;

import javax.swing.*;
import java.util.List;

public abstract class ROIManagerPluginSelectionContextPanel extends JPanel {

    private final ROIManagerPlugin roiManagerPlugin;

    public ROIManagerPluginSelectionContextPanel(ROIManagerPlugin roiManagerPlugin) {
        this.roiManagerPlugin = roiManagerPlugin;
    }

    public ROIManagerPlugin getRoiManagerPlugin() {
        return roiManagerPlugin;
    }

    public ImageViewerPanel getViewerPanel() {
        return roiManagerPlugin.getViewerPanel();
    }

    public abstract void selectionUpdated(ROIListData allROI, List<Roi> selectedROI);
}
