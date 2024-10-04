package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbenchAccess;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

/**
 * Window that displays data using {@link JIPipeDesktopDataViewer}
 */
public class JIPipeDesktopDataViewerWindow extends JFrame implements JIPipeDesktopProjectWorkbenchAccess {
    private final JIPipeDesktopProjectWorkbench workbench;

    public JIPipeDesktopDataViewerWindow(JIPipeDesktopProjectWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        onDataChanged();
    }

    private void initialize() {
        setIconImage(UIUtils.getJIPipeIcon128());
    }

    private void onDataChanged() {
        setTitle("No data - JIPipe");
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
