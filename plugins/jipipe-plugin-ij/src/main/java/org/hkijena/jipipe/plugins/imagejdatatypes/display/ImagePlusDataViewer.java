package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.imageviewernext.JIPipeDesktopImageViewer;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import java.awt.*;

public class ImagePlusDataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopImageViewer imageViewer;

    public ImagePlusDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        this.imageViewer = new JIPipeDesktopImageViewer(dataViewerWindow.getDesktopWorkbench());
        initialize();
    }

    private void initialize() {
        add(imageViewer, BorderLayout.CENTER);
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        imageViewer.buildRibbon(ribbon);
    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {
        imageViewer.buildDock(dockPanel);
    }

    @Override
    public void postOnDataChanged() {
        imageViewer.startRenderer();
    }
}
