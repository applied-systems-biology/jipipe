package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerLUTEditor;
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

    public List<ImageViewerLUTEditor> getLutEditors() {
        return lutEditors;
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
