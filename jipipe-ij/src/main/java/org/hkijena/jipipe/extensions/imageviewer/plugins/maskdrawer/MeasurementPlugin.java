package org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.plugins.ImageViewerPanelPlugin;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/**
 * Applies measurements on a drawn mask
 */
public class MeasurementPlugin extends ImageViewerPanelPlugin implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private MaskDrawerPlugin maskDrawerPlugin;
    private JXTable table;
    private JToggleButton autoMeasureToggle;
    private ImageStatisticsSetParameter statistics = new ImageStatisticsSetParameter();

    private boolean measureInPhysicalUnits = true;

    public MeasurementPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        statistics.setCollapsed(false);
        statistics.getValues().add(Measurement.PixelValueMean);
        initialize();
        viewerPanel.getCanvas().getEventBus().register(this);
    }

    private void initialize() {
        this.autoMeasureToggle = new JToggleButton(UIUtils.getIconFromResources("actions/view-refresh.png"));
        this.autoMeasureToggle.setSelected(true);
        this.autoMeasureToggle.setToolTipText("Update automatically");
        this.autoMeasureToggle.addActionListener(e -> {
            if (autoMeasureToggle.isSelected()) {
                measureCurrentMask();
            }
        });
        table = new JXTable();
    }

    public MaskDrawerPlugin getMaskDrawerPlugin() {
        if (maskDrawerPlugin == null) {
            for (ImageViewerPanelPlugin plugin : getViewerPanel().getPlugins()) {
                if (plugin instanceof MaskDrawerPlugin) {
                    maskDrawerPlugin = (MaskDrawerPlugin) plugin;
                    break;
                }
            }
        }
        return maskDrawerPlugin;
    }

    public void measureCurrentMask() {
        if (getCurrentImage() == null) {
            showNoMeasurements();
            return;
        }
        if (getMaskDrawerPlugin() == null) {
            showNoMeasurements();
            return;
        }
        if (getViewerPanel().getCurrentSlice() == null) {
            showNoMeasurements();
            return;
        }
        ImageProcessor ip = getMaskDrawerPlugin().getCurrentMaskSlice();
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
                statistics, false, measureInPhysicalUnits);
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
    public void createPalettePanel(FormPanel formPanel) {
        FormPanel.GroupHeaderPanel groupHeader = formPanel.addGroupHeader("Measure mask", UIUtils.getIconFromResources("actions/measure.png"));

        JButton measureButton = new JButton("Measure", UIUtils.getIconFromResources("actions/calculator.png"));
        measureButton.addActionListener(e -> measureCurrentMask());
        groupHeader.addColumn(measureButton);

        JButton configureButton = new JButton(UIUtils.getIconFromResources("actions/configure.png"));
        configureButton.addActionListener(e -> showSettings());
        groupHeader.addColumn(configureButton);

        groupHeader.addColumn(autoMeasureToggle);
        formPanel.addWideToForm(table, null);
    }

    private void showSettings() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getViewerPanel()));
        dialog.setTitle("Measurement settings");
        dialog.setContentPane(new ParameterPanel(new JIPipeDummyWorkbench(), this, null, FormPanel.WITH_SCROLLING));
        UIUtils.addEscapeListener(dialog);
        dialog.setSize(640, 480);
        dialog.setLocationRelativeTo(getViewerPanel());
        dialog.revalidate();
        dialog.repaint();
        dialog.setVisible(true);
    }

    private void showNoMeasurements() {
        table.setModel(new DefaultTableModel());
    }

    @Override
    public String getCategory() {
        return "Measure";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("actions/measure.png");
    }

    @Subscribe
    public void onMaskChanged(MaskDrawerPlugin.MaskChangedEvent event) {
        if (autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
    }

    @JIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
    @JIPipeParameter("statistics")
    public ImageStatisticsSetParameter getStatistics() {
        return statistics;
    }

    @JIPipeParameter("statistics")
    public void setStatistics(ImageStatisticsSetParameter statistics) {
        this.statistics = statistics;
        if (autoMeasureToggle.isSelected()) {
            measureCurrentMask();
        }
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
