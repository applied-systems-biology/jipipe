package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ROIManagerPlugin;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Arrays;

/**
 * Mask drawer focuses on measurements (different category and icon)
 */
public class MeasurementDrawerPlugin extends MaskDrawerPlugin {
    public MeasurementDrawerPlugin(ImageViewerPanel viewerPanel) {
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
    public void createPalettePanel(FormPanel formPanel) {
        super.createPalettePanel(formPanel);

        JButton clearButton = new JButton("Clear", UIUtils.getIconFromResources("actions/clear_left.png"));
        clearButton.addActionListener(e -> clearCurrentMask());
        getCurrentGroupHeader().addColumn(clearButton);

        ROIManagerPlugin roiManager = getViewerPanel().getPlugin(ROIManagerPlugin.class);
        if (roiManager != null) {
            JButton addAsROIButton = new JButton("Add to ROI manager", UIUtils.getIconFromResources("actions/list-add.png"));
            addAsROIButton.addActionListener(e -> addToROIManager());
            getCurrentGroupHeader().addColumn(addAsROIButton);
        }
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
        ROIManagerPlugin roiManager = getViewerPanel().getPlugin(ROIManagerPlugin.class);
        roiManager.importROIs(rois);
        clearCurrentMask();
    }

    @Override
    public String getCategory() {
        return "Measure";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/measure.png");
    }

    public static void main(String[] args) {
//        ImagePlus img = IJ.openImage("E:\\Projects\\JIPipe\\testdata\\ATTC_IµL_3rdReplicate-Experiment-5516\\in\\data.tif");
        ImagePlus img = IJ.openImage("/fastdata/projects/JIPipe/testdata/ATTC_IµL_3rdReplicate-Experiment-5518/in/data.tif");
        JIPipeUITheme.ModernLight.install();
        JFrame frame = new JFrame();
        ImageViewerPanel panel = new ImageViewerPanel();
        MeasurementDrawerPlugin maskDrawerPlugin = new MeasurementDrawerPlugin(panel);
        MeasurementPlugin measurementPlugin = new MeasurementPlugin(panel);
        panel.setPlugins(Arrays.asList(maskDrawerPlugin, measurementPlugin));
        panel.setImage(img);
//        maskDrawerPlugin.setMask(IJ.createImage("Mask", img.getWidth(), img.getHeight(), 1, 8));
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1280, 1024);
        frame.setVisible(true);
    }
}
