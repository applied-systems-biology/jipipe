package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.scijava.Disposable;

/**
 * A viewer for {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public abstract class JIPipeDesktopDataViewer extends JIPipeDesktopWorkbenchPanel implements Disposable {
    private final JIPipeDesktopDataViewerWindow dataViewerWindow;

    public JIPipeDesktopDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow.getDesktopWorkbench());
        this.dataViewerWindow = dataViewerWindow;
    }

    public JIPipeDesktopDataViewerWindow getDataViewerWindow() {
        return dataViewerWindow;
    }

    /**
     * Called after preOnDataChanged() in the phase where the ribbon is re-built
     * @param ribbon the ribbon
     */
    public abstract void rebuildRibbon(JIPipeDesktopRibbon ribbon);

    /**
     * Called before rebuildRibbon()
     * @param dockPanel the dock panel
     */
    public abstract void rebuildDock(JIPipeDesktopDockPanel dockPanel);

    /**
     * Called when the data was changed
     */
    public abstract void preOnDataChanged();

    /**
     * Called after rebuildRibbon() and rebuildDock()
     */
    public abstract void postOnDataChanged();
}
