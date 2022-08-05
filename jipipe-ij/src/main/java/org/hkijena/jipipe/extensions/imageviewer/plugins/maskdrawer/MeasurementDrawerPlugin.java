package org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager.ROIManagerPlugin;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Arrays;

/**
 * Mask drawer focuses on measurements (different category and icon)
 */
public class MeasurementDrawerPlugin extends MaskDrawerPlugin {

    public static final Settings SETTINGS = new Settings();

    private JXTable table;
    private JToggleButton autoMeasureToggle;
    private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();

    public MeasurementDrawerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        viewerPanel.getCanvas().getEventBus().register(this);
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
    }

    private void showSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new ParameterPanel(new JIPipeDummyWorkbench(), SETTINGS, null, FormPanel.WITH_SCROLLING));
        UIUtils.addEscapeListener(dialog);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.revalidate();
        dialog.repaint();
        dialog.setVisible(true);
        if(autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
    }

    private void showNoMeasurements() {
        table.setModel(new DefaultTableModel());
    }

    public void measureCurrentMask() {
        if (getCurrentImage() == null) {
            showNoMeasurements();
            return;
        }
        if (getViewerPanel().getCurrentSlice() == null) {
            showNoMeasurements();
            return;
        }
        ImageProcessor ip = getCurrentMaskSlice();
        if (ip == null) {
            showNoMeasurements();
            return;
        }
        int threshold = ip.isInvertedLut() ? 255 : 0;
        threshold = (threshold == 255) ? 0 : 255;
        ip.setThreshold(threshold, threshold, ImageProcessor.NO_LUT_UPDATE);
        Roi roi = ThresholdToSelection.run(new ImagePlus("slice", ip));
        if (roi == null) {
            roi = new Roi(0, 0, getCurrentImage().getWidth(), getCurrentImage().getHeight());
        }
        ROIListData data = new ROIListData();
        data.add(roi);
        ImagePlus dummy = new ImagePlus("Reference", getViewerPanel().getCurrentSlice().duplicate());
        dummy.setCalibration(getCurrentImage().getCalibration());
        ResultsTableData measurements = data.measure(dummy,
                statistics, false, SETTINGS.measureInPhysicalUnits);
        if (measurements.getRowCount() != 1) {
            showNoMeasurements();
            return;
        }
        ResultsTableData transposed = new ResultsTableData();
        transposed.addStringColumn("Measurement");
        transposed.addNumericColumn("Value");
        for (int col = 0; col < measurements.getColumnCount(); col++) {
            transposed.addRow();
            transposed.setValueAt(measurements.getColumnName(col), transposed.getRowCount() - 1, 0);
            transposed.setValueAt(measurements.getValueAt(0, col), transposed.getRowCount() - 1, 1);
        }
        table.setModel(transposed);
    }

    @Override
    public String getCategory() {
        return "Measure";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/measure.png");
    }

    public static class Settings extends AbstractJIPipeParameterCollection {

        private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();
        private boolean measureInPhysicalUnits = true;

        @JIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
        @JIPipeParameter("statistics")
        public ImageStatisticsSetParameter getStatistics() {
            return statistics;
        }

        @JIPipeParameter("statistics")
        public void setStatistics(ImageStatisticsSetParameter statistics) {
            this.statistics = statistics;
        }

        @JIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
        @JIPipeParameter("measure-in-physical-units")
        public boolean isMeasureInPhysicalUnits() {
            return measureInPhysicalUnits;
        }

        @JIPipeParameter("measure-in-physical-units")
        public void setMeasureInPhysicalUnits(boolean measureInPhysicalUnits) {
            this.measureInPhysicalUnits = measureInPhysicalUnits;
        }
    }
}
