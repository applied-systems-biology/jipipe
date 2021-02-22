package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanelDisplayRangeControl;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CalibrationPlugin extends ImageViewerPanelPlugin {

    private ImageViewerPanelDisplayRangeControl displayRangeCalibrationControl;
    private JComboBox<ImageJCalibrationMode> calibrationModes;
//    private JToggleButton autoCalibrateButton = new JToggleButton("Keep auto-calibrating", UIUtils.getIconFromResources("actions/view-refresh.png"));

    public CalibrationPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
        initialize();
    }

    private void initialize() {
        calibrationModes = new JComboBox<>();
        calibrationModes.setModel(new DefaultComboBoxModel<>(ImageJCalibrationMode.values()));
        calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
        displayRangeCalibrationControl = new ImageViewerPanelDisplayRangeControl(this);
        calibrationModes.addActionListener(e -> {
            displayRangeCalibrationControl.applyCalibration(true);
        });
//        autoCalibrateButton.addActionListener(e -> {
//            if (autoCalibrateButton.isSelected()) {
//                if (calibrationModes.getSelectedItem() != ImageJCalibrationMode.AutomaticImageJ) {
//                    calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
//                } else {
//                    displayRangeCalibrationControl.applyCalibration(true);
//                }
//            }
//        });
    }

    @Override
    public void onImageChanged() {
        displayRangeCalibrationControl.applyCalibration(false);
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Display range", UIUtils.getIconFromResources("actions/contrast.png"));
//        headerPanel.addColumn(autoCalibrateButton);
        formPanel.addToForm(calibrationModes, new JLabel("Calibration type"), null);
        formPanel.addWideToForm(displayRangeCalibrationControl, null);
    }

    @Override
    public ImageProcessor draw(int z, int c, int t, ImageProcessor processor) {
        return processor;
    }

    @Override
    public void onSliceChanged() {
        displayRangeCalibrationControl.applyCalibration(false);
//        displayRangeCalibrationControl.updateSliders();
    }

    @Override
    public void beforeDraw(int z, int c, int t) {

    }

    public ImageJCalibrationMode getSelectedCalibration() {
        return (ImageJCalibrationMode) calibrationModes.getSelectedItem();
    }

    public void setSelectedCalibration(ImageJCalibrationMode mode) {
        calibrationModes.setSelectedItem(mode);
    }

    public void applyCalibrationTo(ImagePlus foreign) {
        displayRangeCalibrationControl.applyCalibration(foreign);
    }

//    public void disableAutoCalibration() {
//        autoCalibrateButton.setSelected(false);
//    }

}
