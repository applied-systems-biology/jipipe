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

package org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.maskdrawer;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.plugins2d.roimanager.ROIManagerPlugin2D;

import java.util.Arrays;

/**
 * Mask drawer with function to add ROIs to the ROI manager
 */
public class MaskToROIDrawerPlugin2D extends MaskDrawerPlugin2D {
    public MaskToROIDrawerPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        setMaskGenerator(this::generateMask);
    }

    private ImagePlus generateMask(ImagePlus imagePlus) {
        return IJ.createHyperStack("Mask",
                getCurrentImagePlus().getWidth(),
                getCurrentImagePlus().getHeight(),
                1,
                1,
                1,
                8);
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
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
        ROI2DListData rois = new ROI2DListData(Arrays.asList(manager.getRoisAsArray()));
        ROIManagerPlugin2D roiManager = getViewerPanel().getPlugin(ROIManagerPlugin2D.class);
        roiManager.importROIs(rois, false);
        clearCurrentMask();
    }
}
