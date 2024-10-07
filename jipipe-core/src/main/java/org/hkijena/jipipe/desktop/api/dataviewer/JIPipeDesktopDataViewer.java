package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.browser.JIPipeDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeDataTableBrowser;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchPanel;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import org.scijava.Disposable;

import java.awt.*;

/**
 * A viewer for {@link org.hkijena.jipipe.api.data.JIPipeData}
 */
public abstract class JIPipeDesktopDataViewer extends JIPipeDesktopWorkbenchPanel implements Disposable {
    private final JIPipeDesktopDataViewerWindow dataViewerWindow;

    public JIPipeDesktopDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow.getDesktopWorkbench());
        this.dataViewerWindow = dataViewerWindow;
        setLayout(new BorderLayout());
    }

    public JIPipeDesktopDataViewerWindow getDataViewerWindow() {
        return dataViewerWindow;
    }

    /**
     * Called after preOnDataChanged() in the phase where the ribbon is re-built
     * @param ribbon the ribbon
     */
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {

    }

    /**
     * Called before rebuildRibbon()
     * @param dockPanel the dock panel
     */
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    /**
     * Called when the data browser was changed.
     */
    public void preOnDataChanged() {

    }

    /**
     * Called when the data browser was changed.
     * Called after rebuildRibbon() and rebuildDock()
     */
    public void postOnDataChanged() {

    }

    /**
     * Called when the full data was downloaded.
     * @param data the downloaded data. please check using instanceof if the correct data is present
     */
    public void onDataDownloaded(JIPipeData data) {

    }


    /**
     * Gets the current data browser
     * @return the data browser. Can be null
     */
    public JIPipeDataBrowser getDataBrowser() {
        return dataViewerWindow.getDataBrowser();
    }

    /**
     * Gets the current data table browser
     * @return the data table browser. Can be null.
     */
    public JIPipeDataTableBrowser getDataTableBrowser() {
        return dataViewerWindow.getDataTableBrowser();
    }
}
