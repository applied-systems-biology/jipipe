package org.hkijena.jipipe.extensions.imageviewer.plugins;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanel;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelDisplayRangeControl;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CalibrationPlugin extends GeneralImageViewerPanelPlugin {

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
            uploadSliceToCanvas();
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
        if (getCurrentImage() != null && getCurrentImage().getType() == ImagePlus.COLOR_RGB) {
            // Set to 0-255
            calibrationModes.setSelectedItem(ImageJCalibrationMode.Depth8Bit);
        }
        displayRangeCalibrationControl.updateFromCurrentSlice(true);
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
        FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("Display range", UIUtils.getIconFromResources("actions/contrast.png"));
//        headerPanel.addColumn(autoCalibrateButton);
        formPanel.addToForm(calibrationModes, new JLabel("Calibration type"), null);
        formPanel.addWideToForm(displayRangeCalibrationControl, null);
    }

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        ImageJUtils.calibrate(processor,
                getSelectedCalibration(),
                displayRangeCalibrationControl.getCustomMin(),
                displayRangeCalibrationControl.getCustomMax(),
                getViewerPanel().getSliceStats(new ImageSliceIndex(c, z, t)));
        return processor;
    }

    @Override
    public void onSliceChanged(boolean deferUploadSlice) {
        displayRangeCalibrationControl.updateFromCurrentSlice(false);
//        displayRangeCalibrationControl.applyCalibration(false);
//        displayRangeCalibrationControl.updateSliders();
    }

    @Override
    public void beforeDraw(int c, int z, int t) {
//        displayRangeCalibrationControl.applyCalibration(false);
    }

    public ImageJCalibrationMode getSelectedCalibration() {
        return (ImageJCalibrationMode) calibrationModes.getSelectedItem();
    }

    public void setSelectedCalibration(ImageJCalibrationMode mode) {
        calibrationModes.setSelectedItem(mode);
    }


//    public void disableAutoCalibration() {
//        autoCalibrateButton.setSelected(false);
//    }

}
