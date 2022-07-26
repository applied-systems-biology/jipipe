package org.hkijena.jipipe.extensions.imageviewer.plugins.roimanager;

import com.google.common.eventbus.EventBus;
import ij.gui.Roi;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.ImageStatisticsSetParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.measure.Measurement;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeDummyWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.List;

public class ROIManagerPluginMeasureContextPanel extends ROIManagerPluginSelectionContextPanel implements JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private static ImageStatisticsSetParameter STATISTICS = new ImageStatisticsSetParameter();
    private boolean measureInPhysicalUnits = true;

    private final JLabel roiInfoLabel = new JLabel();

    static {
        STATISTICS.setCollapsed(false);
        STATISTICS.getValues().add(Measurement.PixelValueMean);
    }

    public ROIManagerPluginMeasureContextPanel(ROIManagerPlugin roiManagerPlugin) {
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

        JButton measureButton = new JButton("Measure", UIUtils.getIconFromResources("actions/statistics.png"));
        measureButton.addActionListener(e -> measure());
        add(measureButton);

        JButton settingsButton = new JButton(UIUtils.getIconFromResources("actions/configure.png"));
        settingsButton.setToolTipText("Configure measurements");
        UIUtils.makeFlat25x25(settingsButton);
        settingsButton.addActionListener(e -> showSettings());
        add(settingsButton);
    }

    private void measure() {
        ROIListData data = getRoiManagerPlugin().getSelectedROIOrAll("Measure", "Please select which ROI you want to measure");
        ResultsTableData measurements = data.measure(ImageJUtils.duplicate(getViewerPanel().getImage()),
                STATISTICS, true, measureInPhysicalUnits);
        TableEditor.openWindow(getViewerPanel().getWorkbench(), measurements, "Measurements");
    }

    @Override
    public void selectionUpdated(ROIListData allROI, List<Roi> selectedROI) {
        if (selectedROI.isEmpty())
            roiInfoLabel.setText(allROI.size() + " ROI");
        else
            roiInfoLabel.setText(selectedROI.size() + "/" + allROI.size() + " ROI");
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @JIPipeDocumentation(name = "Statistics", description = "The statistics to measure")
    @JIPipeParameter("statistics")
    public ImageStatisticsSetParameter getStatistics() {
        return STATISTICS;
    }

    @JIPipeParameter("statistics")
    public void setStatistics(ImageStatisticsSetParameter statistics) {
        this.STATISTICS = statistics;
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
