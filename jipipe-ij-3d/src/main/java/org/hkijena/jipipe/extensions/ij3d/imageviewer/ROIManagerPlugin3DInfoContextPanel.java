package org.hkijena.jipipe.extensions.ij3d.imageviewer;

import ij.gui.Roi;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3D;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class ROIManagerPlugin3DInfoContextPanel extends ROIManagerPlugin3DSelectionContextPanel {
    private final JLabel roiInfoLabel = new JLabel();


    public ROIManagerPlugin3DInfoContextPanel(ROIManagerPlugin3D roiManagerPlugin) {
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
    public void selectionUpdated(ROI3DListData allROI, List<ROI3D> selectedROI) {
        if (selectedROI.isEmpty())
            roiInfoLabel.setText(allROI.size() + " ROI");
        else
            roiInfoLabel.setText(selectedROI.size() + "/" + allROI.size() + " ROI");
    }
}
