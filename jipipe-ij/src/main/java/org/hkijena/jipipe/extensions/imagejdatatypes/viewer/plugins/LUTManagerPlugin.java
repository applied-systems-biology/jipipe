package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerLUTEditor;
import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.ImageViewerPanel;
import org.hkijena.jipipe.ui.components.DocumentChangeListener;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.ArrayList;
import java.util.List;

public class LUTManagerPlugin extends GeneralImageViewerPanelPlugin {

    private List<ImageViewerLUTEditor> lutEditors = new ArrayList<>();

    public LUTManagerPlugin(ImageViewerPanel viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void onImageChanged() {
    }

    @Override
    public void createPalettePanel(FormPanel formPanel) {
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
                ImageViewerLUTEditor editor = new ImageViewerLUTEditor(getViewerPanel(), lutEditors.size());
                editor.loadLUTFromImage();
                lutEditors.add(editor);
            }
            FormPanel.GroupHeaderPanel headerPanel = formPanel.addGroupHeader("LUT", UIUtils.getIconFromResources("actions/color-gradient.png"));
            if (getCurrentImage().getNChannels() == 3) {
                JButton toRGBButton = new JButton("Convert to RGB", UIUtils.getIconFromResources("actions/colors-rgb.png"));
                headerPanel.add(toRGBButton);
                toRGBButton.addActionListener(e -> convertImageToRGB());
            }
            for (int channel = 0; channel < getCurrentImage().getNChannels(); channel++) {
                ImageViewerLUTEditor editor = lutEditors.get(channel);
                JTextField channelNameEditor = new JTextField(editor.getChannelName());
                channelNameEditor.setOpaque(false);
                channelNameEditor.setBorder(null);
                channelNameEditor.getDocument().addDocumentListener(new DocumentChangeListener() {
                    @Override
                    public void changed(DocumentEvent documentEvent) {
                        editor.setChannelName(channelNameEditor.getText());
                    }
                });
                formPanel.addToForm(editor, channelNameEditor, null);
            }
        }

        // Apply LUT after creating the panel
        for (int i = 0; i < Math.min(getCurrentImage().getNChannels(), lutEditors.size()); i++) {
            lutEditors.get(i).applyLUT();
        }
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

    @Override
    public ImageProcessor draw(int c, int z, int t, ImageProcessor processor) {
        if (!(processor instanceof ColorProcessor)) {
            if (c <= lutEditors.size() - 1) {
                processor.setLut(lutEditors.get(c).getLUT());
            }
        }

        // Workaround: setting LUT overrides calibration for some reason
        // Recalibrate again
        CalibrationPlugin calibrationPlugin = getViewerPanel().getPlugin(CalibrationPlugin.class);
        if(calibrationPlugin != null) {
            calibrationPlugin.draw(c,z,t, processor);
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
