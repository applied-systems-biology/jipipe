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



    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
