package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.ribbon.LargeButtonAction;
import org.hkijena.jipipe.ui.components.ribbon.Ribbon;
import org.hkijena.jipipe.ui.components.ribbon.SmallButtonAction;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Mask drawer focuses on measurements (different category and icon)
 */
public class MeasurementDrawerPlugin2D extends MaskDrawerPlugin2D {

    public static final Settings SETTINGS = new Settings();
    private final JCheckBox autoMeasureToggle = new JCheckBox("Measure on changes");
    private JXTable table = new JXTable();
    private ResultsTableData lastMeasurements;

    public MeasurementDrawerPlugin2D(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        initialize();
        viewerPanel.getViewerPanel2D().getCanvas().getEventBus().register(this);
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

        // Add the measurement table
        formPanel.addWideToForm(table);
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
        if (autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
    }

    private void initialize() {
        Ribbon.Task measureTask = getRibbon().addTask("Measure");
        getRibbon().reorderTasks(Arrays.asList("Draw", "Measure"));

        Ribbon.Band generalBand = measureTask.addBand("General");
        generalBand.add(new LargeButtonAction("Measure", "Measures the image/mask now", UIUtils.getIcon32FromResources("actions/statistics.png"), this::measureCurrentMask));
        generalBand.add(new SmallButtonAction("Settings ...", "Opens the settings for the measurement tool", UIUtils.getIconFromResources("actions/configure.png"), this::showSettings));
        generalBand.add(new Ribbon.Action(autoMeasureToggle, 1, new Insets(2, 2, 2, 2)));

        Ribbon.Task importExportTask = getRibbon().getOrCreateTask("Import/Export");
        Ribbon.Band importExportMeasurementsBand = importExportTask.addBand("Measurements");
        importExportMeasurementsBand.add(new SmallButtonAction("Export to file", "Exports the measurements to *.csv/*.xlsx", UIUtils.getIconFromResources("actions/save.png"), this::exportMeasurementsToFile));
        importExportMeasurementsBand.add(new SmallButtonAction("Open in editor", "Opens the measurements in a table editor", UIUtils.getIconFromResources("actions/link.png"), this::exportMeasurementsToEditor));

        autoMeasureToggle.setSelected(true);
//        table.setBorder(new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 3));
    }

    private void exportMeasurementsToEditor() {
        if (lastMeasurements == null) {
            return;
        }
        TableEditor.openWindow(getWorkbench(), new ResultsTableData(lastMeasurements), "Measurements");
    }

    private void exportMeasurementsToFile() {
        if (lastMeasurements == null) {
            return;
        }
        Path selectedPath = FileChooserSettings.saveFile(getViewerPanel(), FileChooserSettings.LastDirectoryKey.Projects, "Export table", UIUtils.EXTENSION_FILTER_CSV, UIUtils.EXTENSION_FILTER_XLSX);
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
        if (getCurrentImage() == null) {
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
            roi = new Roi(0, 0, getCurrentImage().getWidth(), getCurrentImage().getHeight());
        }
        ROIListData data = new ROIListData();
        data.add(roi);
        ImagePlus dummy = new ImagePlus("Reference", getViewerPanel2D().getCurrentSlice().duplicate());
        dummy.setCalibration(getCurrentImage().getCalibration());
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

    @Subscribe
    public void onMaskChanged(MaskDrawerPlugin2D.MaskChangedEvent event) {
        if (autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
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
