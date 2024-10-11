package org.hkijena.jipipe.plugins.imageviewer.vtk.tools;

import org.hkijena.jipipe.plugins.imageviewer.vtk.JIPipeDesktopVtkImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.vtk.JIPipeDesktopVtkImageViewerTool;
import org.hkijena.jipipe.plugins.imageviewer.vtk.VtkPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class ViewZoomTool implements JIPipeDesktopVtkImageViewerTool {
    @Override
    public String getLabel() {
        return "Zoom";
    }

    @Override
    public String getDescription() {
        return "TODO";
    }

    @Override
    public Icon getIcon32() {
        return UIUtils.getIcon32FromResources("actions/magnifying-glass.png");
    }

    @Override
    public Icon getIcon16() {
        return UIUtils.getIcon16FromResources("actions/magnifying-glass.png");
    }

    @Override
    public void onActivate(JIPipeDesktopVtkImageViewer imageViewer) {

    }

    @Override
    public void onDeactivate(JIPipeDesktopVtkImageViewer imageViewer) {

    }

    @Override
    public VtkPanel.InteractionTool getInteractionTool() {
        return VtkPanel.InteractionTool.Zoom;
    }
}
