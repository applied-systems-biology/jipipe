package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewer;
import org.hkijena.jipipe.extensions.imageviewer.JIPipeImageViewerPlugin3D;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
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
    public void initializeSettingsPanel(FormPanel formPanel) {
        FormPanel.GroupHeaderPanel groupHeaderPanel = formPanel.addGroupHeader("Image rendering", UIUtils.getIconFromResources("actions/viewimage.png"));
        JButton saveAsDefaultButton = new JButton("Save as default", UIUtils.getIconFromResources("actions/save.png"));
        saveAsDefaultButton.addActionListener(e -> saveImageRenderSettingsAsDefault());
        groupHeaderPanel.addColumn(saveAsDefaultButton);
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), getViewerPanel3D().getImage3DRendererSettings(), null, ParameterPanel.NO_GROUP_HEADERS);
        formPanel.addWideToForm(parameterPanel);
    }

    private void saveImageRenderSettingsAsDefault() {
        getViewerPanel3D().getSettings().getRendererSettings().copyFrom(getViewerPanel3D().getImage3DRendererSettings());
        JIPipe.getSettings().save();
    }
}
