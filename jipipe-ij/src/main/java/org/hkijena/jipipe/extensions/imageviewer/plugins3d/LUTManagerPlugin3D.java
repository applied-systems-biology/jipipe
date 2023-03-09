package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerGrayscaleLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.ImageViewer3DGreyscaleLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.ImageViewer3DLUTEditor;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.ArrayList;
import java.util.List;

public class LUTManagerPlugin3D extends GeneralImageViewerPanelPlugin3D {

    private final List<ImageViewerLUTEditor> lutEditors = new ArrayList<>();

    private final List<ImageViewerGrayscaleLUTEditor> alphaLutEditors = new ArrayList<>();

    public LUTManagerPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void onImageChanged() {
        lutEditors.clear();
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImage() == null) {
            return;
        }
        if (getCurrentImage().getType() == ImagePlus.COLOR_256 || getCurrentImage().getType() == ImagePlus.COLOR_RGB) {
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            JButton toRGBButton = new JButton("Split channels", UIUtils.getIconFromResources("actions/channelmixer.png"));
            headerPanel.add(toRGBButton);
            toRGBButton.addActionListener(e -> splitChannels());
        } else {
            while (lutEditors.size() < getCurrentImage().getNChannels()) {
                ImageViewerLUTEditor editor = new ImageViewer3DLUTEditor(getViewerPanel(), lutEditors.size());
                editor.loadLUTFromImage();
                lutEditors.add(editor);

                ImageViewer3DGreyscaleLUTEditor alphaEditor = new ImageViewer3DGreyscaleLUTEditor(getViewerPanel(), lutEditors.size());
                alphaLutEditors.add(alphaEditor);
            }
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            if (getCurrentImage().getNChannels() == 3) {
                JButton toRGBButton = new JButton("Convert to RGB", UIUtils.getIconFromResources("actions/colors-rgb.png"));
                headerPanel.add(toRGBButton);
                toRGBButton.addActionListener(e -> convertImageToRGB());
            }
            for (int channel = 0; channel < getCurrentImage().getNChannels(); channel++) {
                ImageViewerLUTEditor editor = lutEditors.get(channel);
                formPanel.addToForm(editor, new JLabel("Channel " + (channel + 1)));

                ImageViewerGrayscaleLUTEditor alphaEditor = alphaLutEditors.get(channel);
                formPanel.addToForm(alphaEditor, new JLabel("Channel " + (channel + 1) + " (Opacity)"));
            }
        }

        // Apply LUT after creating the panel
        getViewerPanel3D().updateLutAndCalibration();
    }

    public List<ImageViewerLUTEditor> getLutEditors() {
        return lutEditors;
    }

    public List<ImageViewerGrayscaleLUTEditor> getAlphaLutEditors() {
        return alphaLutEditors;
    }

    private void splitChannels() {
        if (getCurrentImage() != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                getViewerPanel().setImage(ImageJUtils.rgbToChannels(getCurrentImage()));
            }
        }
    }

    private void convertImageToRGB() {
        if (getCurrentImage() != null) {
            try (BusyCursor cursor = new BusyCursor(getViewerPanel())) {
                getViewerPanel().setImage(ImageJUtils.channelsToRGB(getCurrentImage()));
            }
        }
    }
}