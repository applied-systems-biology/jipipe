package org.hkijena.jipipe.plugins.plots.viewers;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.app.ploteditor.JFreeChartPlotEditor;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.plots.datatypes.JFreeChartPlotData;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import java.awt.*;

public class JFreeChartPlotDataViewer extends JIPipeDesktopDataViewer {

    private JFreeChartPlotEditor plotEditor;

    public JFreeChartPlotDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        initialize();
    }

    private void initialize() {
        this.plotEditor = new JFreeChartPlotEditor(getDesktopWorkbench());
        add(plotEditor, BorderLayout.CENTER);
    }

    @Override
    public void postOnDataChanged() {
        getDataViewerWindow().startDownloadFullData();
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
       plotEditor.rebuildRibbon(ribbon);
    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {
        plotEditor.rebuildDock(dockPanel);
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if (data instanceof JFreeChartPlotData) {
            plotEditor.importExistingPlot((JFreeChartPlotData) data.duplicate(JIPipeProgressInfo.SILENT));
        }
    }
}
