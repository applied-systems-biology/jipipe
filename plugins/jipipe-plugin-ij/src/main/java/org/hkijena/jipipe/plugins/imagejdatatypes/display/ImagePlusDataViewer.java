package org.hkijena.jipipe.plugins.imagejdatatypes.display;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.imagejdatatypes.ImageJDataTypesPlugin;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.settings.ImageViewerGeneralUIApplicationSettings;
import org.hkijena.jipipe.plugins.imageviewer.vtk.JIPipeDesktopVtkImageViewer;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class ImagePlusDataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopVtkImageViewer vtkImageViewer;
    private final JIPipeDesktopLegacyImageViewer legacyImageViewer;

    public ImagePlusDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        if(ImageJDataTypesPlugin.isVtkAvailable() && !ImageViewerGeneralUIApplicationSettings.getInstance().isForceLegacyImageViewer()) {
            this.vtkImageViewer = new JIPipeDesktopVtkImageViewer(dataViewerWindow.getDesktopWorkbench());
            this.legacyImageViewer = null;
        }
        else {
            this.vtkImageViewer = null;
            this.legacyImageViewer = new JIPipeDesktopLegacyImageViewer(dataViewerWindow.getDesktopWorkbench(), JIPipeDesktopLegacyImageViewer.DEFAULT_PLUGINS, Collections.emptyMap());
        }
        initialize();
    }

    private void initialize() {
        if(vtkImageViewer != null) {
            add(vtkImageViewer, BorderLayout.CENTER);
        }
        else {
            add(legacyImageViewer, BorderLayout.CENTER);
        }
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        if(vtkImageViewer != null) {
            vtkImageViewer.buildRibbon(ribbon);
        }
        else {
            legacyImageViewer.buildRibbon(ribbon);
        }
    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {
        if(vtkImageViewer != null) {
            vtkImageViewer.buildDock(dockPanel);
        }
        else {
            legacyImageViewer.buildDock(dockPanel);
        }
    }

    @Override
    public void rebuildStatusBar(JToolBar statusBar) {
        if(vtkImageViewer != null) {
            vtkImageViewer.buildStatusBar(statusBar);
        }
        else {
            legacyImageViewer.buildStatusBar(statusBar);
        }
    }

    @Override
    public void postOnDataChanged() {
        if(vtkImageViewer != null) {
            vtkImageViewer.startRenderer();
        }
        getDataViewerWindow().startDownloadFullData();
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        if(vtkImageViewer != null) {
            loadDataIntoVtkViewer(data);
        }
        else {
            loadDataIntoLegacyViewer(data);
        }
    }

    private void loadDataIntoLegacyViewer(JIPipeData data) {
        if(data instanceof ImagePlusData) {
            legacyImageViewer.setImageData((ImagePlusData) data);
        }
    }

    protected void loadDataIntoVtkViewer(JIPipeData data) {

    }
}
