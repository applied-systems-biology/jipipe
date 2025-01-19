package org.hkijena.jipipe.plugins.core.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.app.datatable.JIPipeDesktopExtendedDataTableUI;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.utils.data.OwningStore;
import org.hkijena.jipipe.utils.data.WeakStore;

import java.awt.*;

public class JIPipeDataTableDataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopExtendedDataTableUI dataTableUI;

    public JIPipeDataTableDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        this.dataTableUI = new JIPipeDesktopExtendedDataTableUI(dataViewerWindow.getDesktopWorkbench(),
                new OwningStore<>(new JIPipeDataTable()),
                false,
                true);
        add(dataTableUI, BorderLayout.CENTER);
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        dataTableUI.buildRibbon(ribbon);
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if(data instanceof JIPipeDataTable) {
            dataTableUI.setDataTable(new WeakStore<>((JIPipeDataTable) data));
        }
    }

    @Override
    public void postOnDataChanged() {
        getDataViewerWindow().startDownloadFullData();
    }
}
