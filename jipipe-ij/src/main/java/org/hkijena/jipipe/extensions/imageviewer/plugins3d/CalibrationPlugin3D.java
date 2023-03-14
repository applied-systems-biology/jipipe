package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.ImageViewer3DDisplayRangeControl;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CalibrationPlugin3D extends GeneralImageViewerPanelPlugin3D {

    private ImageViewer3DDisplayRangeControl displayRangeCalibrationControl;
    private JComboBox<ImageJCalibrationMode> calibrationModes;

    public CalibrationPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        initialize();
    }

    private void initialize() {
        calibrationModes = new JComboBox<>();
        calibrationModes.setModel(new DefaultComboBoxModel<>(ImageJCalibrationMode.values()));
        calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
        displayRangeCalibrationControl = new ImageViewer3DDisplayRangeControl(this);
        calibrationModes.addActionListener(e -> {
            displayRangeCalibrationControl.updateFromCurrentImage(false);
            getViewerPanel3D().scheduleUpdateLutAndCalibration();
        });
    }

    @Override
    public void onImageChanged() {
        if (getCurrentImagePlus() != null) {

            if(getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {
                // Set to 0-255
                calibrationModes.setSelectedItem(ImageJCalibrationMode.Depth8Bit);
            }
            else {
                ImageProcessor processor = getCurrentImagePlus().getProcessor();
                double min = processor.getMin();
                double max = processor.getMax();
                if(min != 0 || max != 0) {
                    boolean found = false;
                    for (ImageJCalibrationMode mode : ImageJCalibrationMode.values()) {
                        if(mode.getMin() == min && mode.getMax() == max) {
                            calibrationModes.setSelectedItem(mode);
                            found = true;
                            break;
                        }
                    }
                    if(!found) {
                        calibrationModes.setSelectedItem(ImageJCalibrationMode.Custom);
                        displayRangeCalibrationControl.updateFromCurrentImage(true);
                        displayRangeCalibrationControl.setCustomMinMax(min, max);
                    }
                }
                else {
                    calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
                }
            }

        }
        displayRangeCalibrationControl.updateFromCurrentImage(true);
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        formPanel.addGroupHeader("Display range", UIUtils.getIconFromResources("actions/contrast.png"));
        formPanel.addToForm(calibrationModes, new JLabel("Calibration type"), null);
        formPanel.addWideToForm(displayRangeCalibrationControl, null);
    }

    public ImageJCalibrationMode getSelectedCalibration() {
        return (ImageJCalibrationMode) calibrationModes.getSelectedItem();
    }

    public void setSelectedCalibration(ImageJCalibrationMode mode) {
        calibrationModes.setSelectedItem(mode);
    }

    public double[] calculateCalibration() {
        if(getCurrentImagePlus() != null) {
            return ImageJUtils.calculateCalibration(getCurrentImagePlus().getProcessor(),
                    (ImageJCalibrationMode) calibrationModes.getSelectedItem(),
                    displayRangeCalibrationControl.getCustomMin(),
                    displayRangeCalibrationControl.getCustomMax(),
                    getViewerPanel3D().getCurrentImageStats());
        }
        else {
            return new double[] {0,255};
        }
    }
}
