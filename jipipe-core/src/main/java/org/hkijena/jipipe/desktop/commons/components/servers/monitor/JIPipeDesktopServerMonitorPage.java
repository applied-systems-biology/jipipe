package org.hkijena.jipipe.desktop.commons.components.servers.monitor;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;

import java.awt.*;

/**
 * Abstract base class for pages within the {@link JIPipeDesktopServerMonitorWindow}.
 * Follows the same page-based design pattern as the AI and cache monitors.
 */
public abstract class JIPipeDesktopServerMonitorPage extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeDesktopServerMonitorWindow window;

    public JIPipeDesktopServerMonitorPage(JIPipeDesktopServerMonitorWindow window) {
        super(window.getDesktopWorkbench());
        this.window = window;
        setLayout(new BorderLayout(8, 8));
    }

    public JIPipeDesktopServerMonitorWindow getWindow() {
        return window;
    }

    /**
     * Refreshes the page content.
     */
    public abstract void refresh();
}
