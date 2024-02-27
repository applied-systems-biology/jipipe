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
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer2d.ImageViewer2DLUTEditor;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class LUTManagerPlugin2D extends GeneralImageViewerPanelPlugin2D {

    private final List<ImageViewerLUTEditor> lutEditors = new ArrayList<>();

    public LUTManagerPlugin2D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void onImageChanged() {
        lutEditors.clear();
        if (getCurrentImage() != null) {
            if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {

            } else {
                while (lutEditors.size() < getCurrentImagePlus().getNChannels()) {
                    ImageViewerLUTEditor editor = new ImageViewer2DLUTEditor(getViewerPanel(), lutEditors.size());
                    editor.loadLUTFromImage();
                    lutEditors.add(editor);
                }
            }
        }
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImagePlus() == null) {
            return;
        }
        if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            JButton toRGBButton = new JButton("Split channels", UIUtils.getIconFromResources("actions/channelmixer.png"));
            headerPanel.add(toRGBButton);
            toRGBButton.addActionListener(e -> splitChannels());
        } else {

            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            if (getCurrentImagePlus().getNChannels() == 3) {
                JButton toRGBButton = new JButton("Convert to RGB", UIUtils.getIconFromResources("actions/colors-rgb.png"));
                headerPanel.add(toRGBButton);
                toRGBButton.addActionListener(e -> convertImageToRGB());
            }
            for (int channel = 0; channel < getCurrentImagePlus().getNChannels(); channel++) {
                ImageViewerLUTEditor editor = lutEditors.get(channel);
                formPanel.addWideToForm(editor);
            }
        }

        // Apply LUT after creating the panel
        for (int i = 0; i < Math.min(getCurrentImagePlus().getNChannels(), lutEditors.size()); i++) {
            lutEditors.get(i).applyLUT();
        }
    }

    private void splitChannels() {
        if (getCurrentImagePlus() != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                ImagePlus imagePlus = ImageJUtils.rgbToChannels(getCurrentImagePlus());
                ImagePlusData imagePlusData = new ImagePlusData(imagePlus);
                imagePlusData.copyMetadata(getCurrentImage());
                getViewerPanel().setImageData(imagePlusData);
            }
        }
    }

    private void convertImageToRGB() {
        if (getCurrentImagePlus() != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                ImagePlus imagePlus = ImageJUtils.channelsToRGB(getCurrentImagePlus());
                ImagePlusData imagePlusData = new ImagePlusData(imagePlus);
                imagePlusData.copyMetadata(getCurrentImage());
                getViewerPanel().setImageData(imagePlusData);
            }
        }
    }

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        if (!(processor instanceof ColorProcessor)) {
            if (c <= lutEditors.size() - 1) {
                processor.setLut(lutEditors.get(c).getLUT());
            }
        }

        // Workaround: setting LUT overrides calibration for some reason
        // Recalibrate again
        CalibrationPlugin2D calibrationPlugin = getViewerPanel().getPlugin(CalibrationPlugin2D.class);
        if (calibrationPlugin != null) {
            calibrationPlugin.draw(c, z, t, processor);
        }

        return processor;
    }

    @Override
    public void beforeDraw(int c, int z, int t) {
//        if(getCurrentImage().getType() != ImagePlus.COLOR_RGB) {
//            if (getCurrentImage() instanceof CompositeImage) {
//                CompositeImage image = (CompositeImage) getCurrentImage();
//                if (c <= lutEditors.size() - 1) {
//                    image.setChannelLut(lutEditors.get(c).getLUT(), c + 1);
//                    image.setLut(lutEditors.get(c).getLUT());
//                }
//            } else {
//                ImagePlus image = getCurrentImage();
//                if (c <= lutEditors.size() - 1) {
//                    image.setLut(lutEditors.get(c).getLUT());
//                }
//            }
//        }
    }
}
