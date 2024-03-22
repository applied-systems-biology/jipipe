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
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class AnimationSpeedPlugin3D extends GeneralImageViewerPanelPlugin3D {
    public AnimationSpeedPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        ImagePlus image = getCurrentImagePlus();
        if (image != null && image.getNFrames() > 1) {
            formPanel.addGroupHeader("Animation", UIUtils.getIconFromResources("actions/filmgrain.png"));
            formPanel.addToForm(getViewerPanel3D().getAnimationFPSControl(), new JLabel("Animation FPS"), null);
        }
    }
}
