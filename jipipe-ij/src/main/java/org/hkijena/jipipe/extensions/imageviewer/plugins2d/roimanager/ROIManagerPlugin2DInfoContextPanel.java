package org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager;

import ij.gui.Roi;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class ROIManagerPlugin2DInfoContextPanel extends ROIManagerPlugin2DSelectionContextPanel {
    private final JLabel roiInfoLabel = new JLabel();


    public ROIManagerPlugin2DInfoContextPanel(ROIManagerPlugin2D roiManagerPlugin) {
        super(roiManagerPlugin);
        initialize();
    }

    private void initialize() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        roiInfoLabel.setIcon(UIUtils.getIconFromResources("data-types/roi.png"));
        roiInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(roiInfoLabel);
        add(Box.createHorizontalGlue());
    }


    @Override
    public void selectionUpdated(ROIListData allROI, List<Roi> selectedROI) {
        if (selectedROI.isEmpty())
            roiInfoLabel.setText(allROI.size() + " ROI");
        else
            roiInfoLabel.setText(selectedROI.size() + "/" + allROI.size() + " ROI");
    }
}
