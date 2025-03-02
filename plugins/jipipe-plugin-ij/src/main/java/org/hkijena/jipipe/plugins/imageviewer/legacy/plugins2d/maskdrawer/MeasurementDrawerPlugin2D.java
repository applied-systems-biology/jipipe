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
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.tableeditor.JIPipeDesktopTableEditor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopLargeButtonRibbonAction;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopSmallButtonRibbonAction;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;

/**
 * Mask drawer focuses on measurements (different category and icon)
 */
public class MeasurementDrawerPlugin2D extends MaskDrawerPlugin2D implements MaskDrawerPlugin2D.MaskChangedEventListener {

    public static final Settings SETTINGS = new Settings();
    private final JCheckBox autoMeasureToggle = new JCheckBox("Measure on changes");
    private final JXTable table = new JXTable();
    private ResultsTableData lastMeasurements;

    public MeasurementDrawerPlugin2D(JIPipeDesktopLegacyImageViewer viewerPanel) {
        super(viewerPanel);
        initialize();
        getMaskChangedEventEmitter().subscribe(this);
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
    public void buildPanel(JIPipeDesktopFormPanel formPanel) {
        super.buildPanel(formPanel);

        // Add the measurement table
        formPanel.addWideToForm(table);
    }

    private void showSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new JIPipeDesktopParameterFormPanel(new JIPipeDesktopDummyWorkbench(), SETTINGS, null, JIPipeDesktopFormPanel.WITH_SCROLLING));
        UIUtils.addEscapeListener(dialog);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.revalidate();
        dialog.repaint();
        dialog.setVisible(true);
        if (autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
    }

    private void initialize() {
        autoMeasureToggle.setSelected(true);
    }

    @Override
    public void buildRibbon(JIPipeDesktopRibbon ribbon) {
        super.buildRibbon(ribbon);

        JIPipeDesktopRibbon.Task maskTask = ribbon.getOrCreateTask("Mask");
        JIPipeDesktopRibbon.Band measureBand = maskTask.getOrCreateBand("Measure");
        measureBand.add(new JIPipeDesktopLargeButtonRibbonAction("Measure", "Measures the image/mask now", UIUtils.getIcon32FromResources("actions/statistics.png"), this::measureCurrentMask));
        measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Settings ...", "Opens the settings for the measurement tool", UIUtils.getIconFromResources("actions/configure.png"), this::showSettings));
        measureBand.add(new JIPipeDesktopRibbon.Action(autoMeasureToggle, 1, new Insets(2, 2, 2, 2)));

        measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Export to file", "Exports the measurements to *.csv/*.xlsx", UIUtils.getIconFromResources("actions/filesave.png"), this::exportMeasurementsToFile));
        measureBand.add(new JIPipeDesktopSmallButtonRibbonAction("Open in editor", "Opens the measurements in a table editor", UIUtils.getIconFromResources("actions/open-in-new-window.png"), this::exportMeasurementsToEditor));

    }

    private void exportMeasurementsToEditor() {
        if (lastMeasurements == null) {
            return;
        }
        JIPipeDesktopTableEditor.openWindow(getDesktopWorkbench(), new ResultsTableData(lastMeasurements), "Measurements");
    }

    private void exportMeasurementsToFile() {
        if (lastMeasurements == null) {
            return;
        }
        Path selectedPath = JIPipeFileChooserApplicationSettings.saveFile(getViewerPanel(), workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Projects, "Export table", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
        if (selectedPath != null) {
            if (UIUtils.EXTENSION_FILTER_XLSX.accept(selectedPath.toFile())) {
                lastMeasurements.saveAsXLSX(selectedPath);
            } else {
                lastMeasurements.saveAsCSV(selectedPath);
            }
        }
    }

    private void showNoMeasurements() {
        table.setModel(new DefaultTableModel());
    }

    public void measureCurrentMask() {
        if (getCurrentImagePlus() == null) {
            showNoMeasurements();
            return;
        }
        if (getViewerPanel2D().getCurrentSlice() == null) {
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
            roi = new Roi(0, 0, getCurrentImagePlus().getWidth(), getCurrentImagePlus().getHeight());
        }
        ROI2DListData data = new ROI2DListData();
        data.add(roi);
        ImagePlus dummy = new ImagePlus("Reference", getViewerPanel2D().getCurrentSlice().duplicate());
        dummy.setCalibration(getCurrentImagePlus().getCalibration());
        ResultsTableData measurements = data.measure(dummy,
                SETTINGS.statistics, false, SETTINGS.measureInPhysicalUnits);
        lastMeasurements = measurements;
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
    public String getPanelName() {
        return "Measure";
    }

    @Override
    public Icon getPanelIcon() {
        return UIUtils.getIcon32FromResources("actions/measure.png");
    }

    @Override
    public void onMaskDrawerPluginMaskChanged(MaskChangedEvent event) {
        if (autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
    }

    public static class Settings extends AbstractJIPipeParameterCollection {

        private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();
        private boolean measureInPhysicalUnits = true;

        @SetJIPipeDocumentation(name = "Statistics", description = "The statistics to measure " + "<br/><br/>" + ImageStatisticsSetParameter.ALL_DESCRIPTIONS)
        @JIPipeParameter("statistics")
        public ImageStatisticsSetParameter getStatistics() {
            return statistics;
        }

        @JIPipeParameter("statistics")
        public void setStatistics(ImageStatisticsSetParameter statistics) {
            this.statistics = statistics;
        }

        @SetJIPipeDocumentation(name = "Measure in physical units", description = "If true, measurements will be generated in physical units if available")
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
