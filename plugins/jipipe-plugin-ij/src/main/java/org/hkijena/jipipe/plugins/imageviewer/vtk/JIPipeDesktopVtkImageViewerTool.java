package org.hkijena.jipipe.plugins.imageviewer.vtk;

import javax.swing.*;

public interface JIPipeDesktopVtkImageViewerTool {
    String getLabel();

    String getDescription();

    Icon getIcon32();

    Icon getIcon16();

    void onActivate(JIPipeDesktopVtkImageViewer imageViewer);

    void onDeactivate(JIPipeDesktopVtkImageViewer imageViewer);

    VtkPanel.InteractionTool getInteractionTool();
}
