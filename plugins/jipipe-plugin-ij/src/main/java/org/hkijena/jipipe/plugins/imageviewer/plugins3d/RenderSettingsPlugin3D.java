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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class RenderSettingsPlugin3D extends JIPipeImageViewerPlugin3D {
    public RenderSettingsPlugin3D(JIPipeImageViewer viewerPanel) {
        super(viewerPanel);
    }

    @Override
    public String getCategory() {
        return "Display";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("devices/video-display.png");
    }

    @Override
    public void initializeSettingsPanel(JIPipeDesktopFormPanel formPanel) {
        JIPipeDesktopFormPanel.GroupHeaderPanel groupHeaderPanel = formPanel.addGroupHeader("Image rendering", UIUtils.getIconFromResources("actions/viewimage.png"));
        JButton saveAsDefaultButton = new JButton("Save as default", UIUtils.getIconFromResources("actions/save.png"));
        saveAsDefaultButton.addActionListener(e -> saveImageRenderSettingsAsDefault());
        groupHeaderPanel.addColumn(saveAsDefaultButton);
        JIPipeDesktopParameterPanel parameterPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(), getViewerPanel3D().getImage3DRendererSettings(), null, JIPipeDesktopParameterPanel.NO_GROUP_HEADERS);
        formPanel.addWideToForm(parameterPanel);
    }

    private void saveImageRenderSettingsAsDefault() {
        getViewerPanel3D().getSettings().getRendererSettings().copyFrom(getViewerPanel3D().getImage3DRendererSettings());
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            JIPipe.getSettings().save();
        }
    }
}
