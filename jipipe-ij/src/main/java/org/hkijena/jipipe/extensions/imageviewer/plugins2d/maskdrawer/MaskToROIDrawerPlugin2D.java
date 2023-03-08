package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.roimanager.ROIManagerPlugin2D;
import org.hkijena.jipipe.ui.components.FormPanel;

import java.util.Arrays;

/**
 * Mask drawer with function to add ROIs to the ROI manager
 */
public class MaskToROIDrawerPlugin2D extends MaskDrawerPlugin2D {
    public MaskToROIDrawerPlugin2D(JIPipeImageViewerPanel viewerPanel) {
        super(viewerPanel);
        setMaskGenerator(this::generateMask);
    }

    private ImagePlus generateMask(ImagePlus imagePlus) {
        return IJ.createHyperStack("Mask",
                getCurrentImage().getWidth(),
                getCurrentImage().getHeight(),
                1,
                1,
                1,
                8);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        super.initializeSettingsPanel(formPanel);

        // TODO

//        getCurrentGroupHeader().setDescription("Please note that you have to click 'Add to ROI manager' to create a ROI.");
//
//        JButton clearButton = new JButton("Clear", UIUtils.getIconFromResources("actions/clear_left.png"));
//        clearButton.addActionListener(e -> clearCurrentMask());
//        getCurrentGroupHeader().addColumn(clearButton);
//
//        ROIManagerPlugin roiManager = getViewerPanel().getPlugin(ROIManagerPlugin.class);
//        if (roiManager != null) {
//            JButton addAsROIButton = new JButton("Add to ROI manager", UIUtils.getIconFromResources("actions/list-add.png"));
//            addAsROIButton.addActionListener(e -> addToROIManager());
//            getCurrentGroupHeader().addColumn(addAsROIButton);
//        }
    }

    private void addToROIManager() {
        RoiManager manager = new RoiManager(true);
        ResultsTable table = new ResultsTable();
        ParticleAnalyzer.setRoiManager(manager);
        ParticleAnalyzer.setResultsTable(table);
        ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.INCLUDE_HOLES,
                0,
                table,
                0,
                Double.POSITIVE_INFINITY,
                0,
                Double.POSITIVE_INFINITY);
        analyzer.analyze(new ImagePlus("mask", getCurrentMaskSlice()));
        ROIListData rois = new ROIListData(Arrays.asList(manager.getRoisAsArray()));
        ROIManagerPlugin2D roiManager = getViewerPanel().getPlugin(ROIManagerPlugin2D.class);
        roiManager.importROIs(rois, false);
        clearCurrentMask();
    }
}
