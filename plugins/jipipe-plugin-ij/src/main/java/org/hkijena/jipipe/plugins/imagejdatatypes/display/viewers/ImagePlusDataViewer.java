/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejdatatypes.display.viewers;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imageviewer.legacy.JIPipeDesktopLegacyImageViewer;
import org.hkijena.jipipe.plugins.imageviewer.legacy.api.JIPipeDesktopLegacyImageViewerPlugin;
import org.hkijena.jipipe.plugins.imageviewer.legacy.impl.JIPipeDesktopLegacyImageViewerPanel2D;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class ImagePlusDataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopLegacyImageViewer legacyImageViewer;
    private boolean panelSetupDone = false;
    private JIPipeData currentData;

    public ImagePlusDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        this.legacyImageViewer = new JIPipeDesktopLegacyImageViewer(dataViewerWindow.getDesktopWorkbench(),
                getLegacyImageViewerPlugins(),
                this,
                Collections.emptyMap());
        initialize();
    }

    protected List<Class<? extends JIPipeDesktopLegacyImageViewerPlugin>> getLegacyImageViewerPlugins() {
        return JIPipeDesktopLegacyImageViewer.DEFAULT_PLUGINS;
    }

    private void initialize() {
        add(legacyImageViewer, BorderLayout.CENTER);
    }

    @Override
    public void rebuildRibbon(JIPipeDesktopRibbon ribbon) {
        legacyImageViewer.buildRibbon(ribbon);
    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {
        legacyImageViewer.buildDock(dockPanel);
    }

    @Override
    public void rebuildStatusBar(JToolBar statusBar) {
        legacyImageViewer.buildStatusBar(statusBar);
    }

    @Override
    public void postOnDataChanged() {
        getDataViewerWindow().startDownloadFullData();
    }

    @Override
    public void onDataDownloaded(JIPipeData data) {
        this.currentData = data;
        loadDataIntoLegacyViewer(data);
        setupPanelsOnce(getDataViewerWindow().getDockPanel());
    }

    @Override
    public void dispose() {
        super.dispose();
        this.legacyImageViewer.dispose();
//        this.vtkImageViewer.dispose();
        this.currentData = null;
    }

    protected void loadDataIntoLegacyViewer(JIPipeData data) {
        if (data instanceof ImagePlusData) {
            legacyImageViewer.setImageData((ImagePlusData) data);
            legacyImageViewer.addOverlays(((ImagePlusData) data).getOverlays());
        }
    }

    protected void setupPanelsOnce(JIPipeDesktopDockPanel dockPanel) {
        if (!panelSetupDone) {
            setupPanels(dockPanel);
            panelSetupDone = true;
        }
    }

    protected void setupPanels(JIPipeDesktopDockPanel dockPanel) {
        activateLegacyDockPanel("General");
    }

    protected void activateLegacyDockPanel(String id) {
        if (legacyImageViewer != null) {
            getDataViewerWindow().getDockPanel().activatePanel(JIPipeDesktopLegacyImageViewerPanel2D.DOCK_PANEL_PREFIX + id, true);
        }
    }

    protected void loadDataIntoVtkViewer(JIPipeData data) {

    }

    public JIPipeDesktopLegacyImageViewer getLegacyImageViewer() {
        return legacyImageViewer;
    }
}
