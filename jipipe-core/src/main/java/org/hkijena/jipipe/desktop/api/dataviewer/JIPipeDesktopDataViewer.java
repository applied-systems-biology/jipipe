package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;

/**
 * A viewer for {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public class JIPipeDesktopDataViewer extends JIPipeDesktopProjectWorkbenchPanel {
    private final JIPipeDesktopDataViewerWindow dataViewerWindow;

    public JIPipeDesktopDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow.getDesktopProjectWorkbench());
        this.dataViewerWindow = dataViewerWindow;
    }

    public JIPipeDesktopDataViewerWindow getDataViewerWindow() {
        return dataViewerWindow;
    }
}
