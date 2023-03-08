package org.hkijena.jipipe.extensions.imageviewer.plugins3d;

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
        return "Rendering";
    }

    @Override
    public Icon getCategoryIcon() {
        return UIUtils.getIconFromResources("devices/video-display.png");
    }

    @Override
    public void initializeSettingsPanel(FormPanel formPanel) {
        ParameterPanel parameterPanel = new ParameterPanel(getWorkbench(), getViewerPanel3D().getImage3DRendererSettings(), null, ParameterPanel.NONE);
        formPanel.addWideToForm(parameterPanel);
    }
}
