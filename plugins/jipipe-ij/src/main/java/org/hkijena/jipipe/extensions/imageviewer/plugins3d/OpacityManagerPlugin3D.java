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

package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import ij.ImagePlus;
import ij3d.Content;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerOpacityLUTEditor;
import org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d.ImageViewer3DOpacityLUTEditor;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class OpacityManagerPlugin3D extends GeneralImageViewerPanelPlugin3D {
    private final List<ImageViewerOpacityLUTEditor> alphaLutEditors = new ArrayList<>();

    public OpacityManagerPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void onImageChanged() {
        alphaLutEditors.clear();
        if (getCurrentImage() != null) {
            if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {

            } else {
                while (alphaLutEditors.size() < getCurrentImagePlus().getNChannels()) {
                    ImageViewerOpacityLUTEditor editor = new ImageViewer3DOpacityLUTEditor(getViewerPanel(), alphaLutEditors.size());
                    alphaLutEditors.add(editor);
                }
            }
        }
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        if (getCurrentImagePlus() == null) {
            return;
        }
        formPanel.addGroupHeader("Opacity", UIUtils.getIconFromResources("actions/edit-opacity.png"));
        if (getCurrentImagePlus().getType() == ImagePlus.COLOR_256 || getCurrentImagePlus().getType() == ImagePlus.COLOR_RGB) {
            if (alphaLutEditors.isEmpty()) {
                alphaLutEditors.add(new ImageViewer3DOpacityLUTEditor(getViewerPanel(), 0));
            }
            ImageViewerOpacityLUTEditor alphaEditor = alphaLutEditors.get(0);
            formPanel.addToForm(alphaEditor, new JLabel("Image"));
        } else {
            for (int channel = 0; channel < getCurrentImagePlus().getNChannels(); channel++) {
                ImageViewerOpacityLUTEditor alphaEditor = alphaLutEditors.get(channel);
                formPanel.addWideToForm(alphaEditor);
            }
        }

        // Apply LUT after creating the panel
        getViewerPanel3D().scheduleUpdateLutAndCalibration();
    }

    @Override
    public void onImageContentReady(List<Content> content) {
//        for (int i = 0; i < content.size(); i++) {
//            Content item = content.get(i);
//            if(item.getTransparency() == 0) {
//                if(i < alphaLutEditors.size()) {
//                    ImageViewerGrayscaleLUTEditor lutEditor = alphaLutEditors.get(i);
//                    lutEditor.setToUniformColor(Color.BLACK);
//                }
//            }
//        }
    }

    public List<ImageViewerOpacityLUTEditor> getAlphaLutEditors() {
        return alphaLutEditors;
    }
}
