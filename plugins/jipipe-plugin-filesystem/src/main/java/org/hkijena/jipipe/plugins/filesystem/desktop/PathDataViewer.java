package org.hkijena.jipipe.plugins.filesystem.desktop;

import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

public class PathDataViewer extends JIPipeDesktopDataViewer {

    public PathDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        initialize();
    }

    private void initialize() {
        add(UIUtils.createReadonlyTextPane("test"));
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {

    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    @Override
    public void preOnDataChanged() {

    }

    @Override
    public void postOnDataChanged() {

    }
}
