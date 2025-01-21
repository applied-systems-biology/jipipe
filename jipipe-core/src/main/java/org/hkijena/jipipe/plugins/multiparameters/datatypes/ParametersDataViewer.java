package org.hkijena.jipipe.plugins.multiparameters.datatypes;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;

import java.awt.*;

public class ParametersDataViewer extends JIPipeDesktopDataViewer {

    private final ParametersDataViewerPanel panel;

    public ParametersDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        this.panel = new ParametersDataViewerPanel(dataViewerWindow.getDesktopWorkbench());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void postOnDataChanged() {
        getDataViewerWindow().startDownloadFullData();
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if (data instanceof ParametersData) {
            panel.setParametersData((ParametersData) data);
        }
    }
}
