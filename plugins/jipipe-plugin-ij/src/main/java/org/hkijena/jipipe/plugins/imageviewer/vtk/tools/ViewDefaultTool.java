package org.hkijena.jipipe.plugins.imageviewer.vtk.tools;

import org.hkijena.jipipe.plugins.imageviewer.vtk.JIPipeDesktopVtkImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.vtk.JIPipeDesktopVtkImageViewerTool;
import org.hkijena.jipipe.plugins.imageviewer.vtk.VtkPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ViewDefaultTool implements JIPipeDesktopVtkImageViewerTool {
    @Override
    public String getLabel() {
        return "Default";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public Icon getIcon32() {
        return UIUtils.getIcon32FromResources("actions/arrow-pointer.png");
    }

    @Override
    public Icon getIcon16() {
        return UIUtils.getIconFromResources("actions/arrow-pointer.png");
    }

    @Override
    public void onActivate(JIPipeDesktopVtkImageViewer imageViewer) {

    }

    @Override
    public void onDeactivate(JIPipeDesktopVtkImageViewer imageViewer) {

    }

    @Override
    public VtkPanel.InteractionTool getInteractionTool() {
        return VtkPanel.InteractionTool.Default;
    }
}
