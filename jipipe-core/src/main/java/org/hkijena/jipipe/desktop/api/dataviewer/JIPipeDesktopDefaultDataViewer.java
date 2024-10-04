package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import java.awt.*;

public class JIPipeDesktopDefaultDataViewer extends JIPipeDesktopDataViewer {
    public JIPipeDesktopDefaultDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(UIUtils.createInfoLabel("Data cannot be displayed",
                "JIPipe has no interface to display this data",
                UIUtils.getIcon32FromResources("actions/circle-xmark.png")), BorderLayout.CENTER);
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
