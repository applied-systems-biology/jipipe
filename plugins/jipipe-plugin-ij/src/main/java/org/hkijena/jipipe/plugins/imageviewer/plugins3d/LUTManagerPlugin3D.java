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

package org.hkijena.jipipe.plugins.imageviewer.plugins3d;

import ij.ImagePlus;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.utils.ImageViewerLUTEditor;
import org.hkijena.jipipe.plugins.imageviewer.utils.viewer3d.ImageViewer3DLUTEditor;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class LUTManagerPlugin3D extends GeneralImageViewerPanelPlugin3D {

    private final List<ImageViewerLUTEditor> lutEditors = new ArrayList<>();

    public LUTManagerPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void onImageChanged() {
        lutEditors.clear();
        if (getCurrentImage() != null) {
            if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {

            } else {
                while (lutEditors.size() < getCurrentImagePlus().getNChannels()) {
                    ImageViewerLUTEditor editor = new ImageViewer3DLUTEditor(getViewerPanel(), lutEditors.size());
                    editor.loadLUTFromImage();
                    lutEditors.add(editor);
                }
            }
        }
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        if (getCurrentImagePlus() == null) {
            return;
        }
        if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {
            JIPipeDesktopFormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            JButton toRGBButton = new JButton("Split channels", UIUtils.getIconFromResources("actions/channelmixer.png"));
            headerPanel.addToTitlePanel(toRGBButton);
            toRGBButton.addActionListener(e -> splitChannels());
        } else {
            JIPipeDesktopFormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            if (getCurrentImagePlus().getNChannels() == 3) {
                JButton toRGBButton = new JButton("Convert to RGB", UIUtils.getIconFromResources("actions/colors-rgb.png"));
                headerPanel.addToTitlePanel(toRGBButton);
                toRGBButton.addActionListener(e -> convertImageToRGB());
            }
            for (int channel = 0; channel < getCurrentImagePlus().getNChannels(); channel++) {
                ImageViewerLUTEditor editor = lutEditors.get(channel);
                formPanel.addWideToForm(editor);
            }
        }

        // Apply LUT after creating the panel
        getViewerPanel3D().scheduleUpdateLutAndCalibration();
    }

    public List<ImageViewerLUTEditor> getLutEditors() {
        return lutEditors;
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
}
