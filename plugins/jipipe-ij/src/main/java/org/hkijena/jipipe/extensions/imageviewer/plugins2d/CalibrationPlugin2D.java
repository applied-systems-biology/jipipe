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

package org.hkijena.jipipe.extensions.imageviewer.plugins2d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewer2DDisplayRangeControl;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public class CalibrationPlugin2D extends GeneralImageViewerPanelPlugin2D {

    private ImageViewer2DDisplayRangeControl displayRangeCalibrationControl;
//    private JComboBox<ImageJCalibrationMode> calibrationModes;
//    private JToggleButton autoCalibrateButton = new JToggleButton("Keep auto-calibrating", UIUtils.getIconFromResources("actions/view-refresh.png"));

    public CalibrationPlugin2D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
        initialize();
    }

    private void initialize() {
//        calibrationModes = new JComboBox<>();
//        calibrationModes.setModel(new DefaultComboBoxModel<>(ImageJCalibrationMode.values()));
//        calibrationModes.setSelectedItem(ImageJCalibrationMode.AutomaticImageJ);
        displayRangeCalibrationControl = new ImageViewer2DDisplayRangeControl(this);
//        calibrationModes.addActionListener(e -> {
//            displayRangeCalibrationControl.updateFromCurrentSlice(false);
//            uploadSliceToCanvas();
//        });
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
        if (getCurrentImagePlus() != null) {

            if (getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {
                // Set to 0-255
                displayRangeCalibrationControl.setMode(ImageJCalibrationMode.Depth8Bit);
            } else {
                ImageProcessor processor = getCurrentImagePlus().getProcessor();
                double min = processor.getMin();
                double max = processor.getMax();
                if ((min != 0 || max != 0) && (!Double.isNaN(min) && !Double.isNaN(max)) && (!Double.isInfinite(min) && !Double.isInfinite(max))) {
                    boolean found = false;
                    for (ImageJCalibrationMode mode : ImageJCalibrationMode.values()) {
                        if (mode.getMin() == min && mode.getMax() == max) {
                            displayRangeCalibrationControl.setMode(mode);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        displayRangeCalibrationControl.setMode(ImageJCalibrationMode.Custom);
                        displayRangeCalibrationControl.updateFromCurrentSlice(true);
                        displayRangeCalibrationControl.setCustomMinMax(min, max);
                    }
                } else {
                    displayRangeCalibrationControl.setMode(ImageJCalibrationMode.AutomaticImageJ);
                }
            }

        }
        displayRangeCalibrationControl.updateFromCurrentSlice(true);
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        formPanel.addGroupHeader("Display range", UIUtils.getIconFromResources("actions/contrast.png"));
//        headerPanel.addColumn(autoCalibrateButton);
//        formPanel.addToForm(calibrationModes, new JLabel("Presets"), null);
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(2, 6, 3, 3));

        // First row
        createModeButton(ImageJCalibrationMode.AutomaticImageJ, buttonsPanel);
        createModeButton(ImageJCalibrationMode.MinMax, buttonsPanel);
        createModeButton(ImageJCalibrationMode.Custom, buttonsPanel);
        buttonsPanel.add(Box.createGlue());
        createModeButton(ImageJCalibrationMode.Depth8Bit, buttonsPanel);
        createModeButton(ImageJCalibrationMode.Depth16Bit, buttonsPanel);

        // Second row

        createModeButton(ImageJCalibrationMode.Depth10Bit, buttonsPanel);
        createModeButton(ImageJCalibrationMode.Depth12Bit, buttonsPanel);
        createModeButton(ImageJCalibrationMode.Depth14Bit, buttonsPanel);
        createModeButton(ImageJCalibrationMode.Depth15Bit, buttonsPanel);
        createModeButton(ImageJCalibrationMode.Unit, buttonsPanel);
        createModeButton(ImageJCalibrationMode.UnitAroundZero, buttonsPanel);

        formPanel.addWideToForm(buttonsPanel);
        formPanel.addWideToForm(displayRangeCalibrationControl, null);
    }

    private void createModeButton(ImageJCalibrationMode mode, JPanel buttonsPanel) {
        JButton switchButton = new JButton(mode.getShortName());
        if (mode == ImageJCalibrationMode.AutomaticImageJ) {
            switchButton.setToolTipText("Set the minimum and maximum display range using ImageJ's auto-calibration algorithm");
        } else if (mode == ImageJCalibrationMode.MinMax) {
            switchButton.setToolTipText("Set the minimum and maximum display range to the minimum and maximum pixel value");
        } else {
            switchButton.setToolTipText("Set the minimum display range to " + mode.getMin() + " and the maximum range to " + mode.getMax());
        }
        switchButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(UIManager.getColor("Button.borderColor"), 1, 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
        switchButton.addActionListener(e -> {
            if (mode == ImageJCalibrationMode.Custom) {
                Optional<double[]> result = UIUtils.getMinMaxByDialog(getViewerPanel2D(), "Custom display range", "Please set the new display range",
                        displayRangeCalibrationControl.getCurrentMin(),
                        displayRangeCalibrationControl.getCurrentMax(),
                        displayRangeCalibrationControl.getMinSelectableValue(),
                        displayRangeCalibrationControl.getMaxSelectableValue());
                if (result.isPresent()) {
                    displayRangeCalibrationControl.setMode(ImageJCalibrationMode.Custom);
                    displayRangeCalibrationControl.setCustomMinMax(result.get()[0], result.get()[1]);
                }
                displayRangeCalibrationControl.applyCustomCalibration();
            } else {
                displayRangeCalibrationControl.setMode(mode);
                displayRangeCalibrationControl.updateFromCurrentSlice(true);
            }
            uploadSliceToCanvas();
        });
        buttonsPanel.add(switchButton);
    }

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        ImageJUtils.calibrate(processor,
                displayRangeCalibrationControl.getMode(),
                displayRangeCalibrationControl.getCustomMin(),
                displayRangeCalibrationControl.getCustomMax(),
                getViewerPanel2D().getSliceStats(new ImageSliceIndex(c, z, t)));
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


//    public void disableAutoCalibration() {
//        autoCalibrateButton.setSelected(false);
//    }

}
