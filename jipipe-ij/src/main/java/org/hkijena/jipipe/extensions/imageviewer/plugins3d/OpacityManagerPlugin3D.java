package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import ij.ImagePlus;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerGrayscaleLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.ImageViewer3DGreyscaleLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.ImageViewer3DLUTEditor;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.BusyCursor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class OpacityManagerPlugin3D extends GeneralImageViewerPanelPlugin3D {
    private final List<ImageViewerGrayscaleLUTEditor> alphaLutEditors = new ArrayList<>();

    public OpacityManagerPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void onImageChanged() {
        alphaLutEditors.clear();
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImagePlus() == null) {
            return;
        }
        formPanel.addGroupHeader("Opacity", UIUtils.getIconFromResources("actions/edit-opacity.png"));
        if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {
            if(alphaLutEditors.isEmpty()) {
                alphaLutEditors.add(new ImageViewer3DGreyscaleLUTEditor(getViewerPanel(), 0));
            }
            ImageViewerGrayscaleLUTEditor alphaEditor = alphaLutEditors.get(0);
            formPanel.addToForm(alphaEditor, new JLabel("Image"));
        } else {
            while (alphaLutEditors.size() < getCurrentImagePlus().getNChannels()) {
                ImageViewerGrayscaleLUTEditor editor = new ImageViewer3DGreyscaleLUTEditor(getViewerPanel(), alphaLutEditors.size());
                alphaLutEditors.add(editor);
            }
            for (int channel = 0; channel < getCurrentImagePlus().getNChannels(); channel++) {
                ImageViewerGrayscaleLUTEditor alphaEditor = alphaLutEditors.get(channel);
                formPanel.addToForm(alphaEditor, new JLabel("Channel " + (channel + 1)));
            }
        }

        // Apply LUT after creating the panel
        getViewerPanel3D().scheduleUpdateLutAndCalibration();
    }

    public List<ImageViewerGrayscaleLUTEditor> getAlphaLutEditors() {
        return alphaLutEditors;
    }
}
