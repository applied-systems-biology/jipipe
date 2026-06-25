package org.hkijena.jipipe.plugins.embeddingserver.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.servers.monitor.JIPipeDesktopServerMonitorWindow;

import javax.swing.*;

/**
 * Menu tool that opens the external server instance monitor window.
 */
public class OpenServerMonitorTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public OpenServerMonitorTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Server monitor");
        setToolTipText("Opens the external server instance monitor");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/database.png"));
        addActionListener(e -> {
            JIPipeDesktopServerMonitorWindow window = new JIPipeDesktopServerMonitorWindow(workbench);
            window.setLocationRelativeTo(workbench.getWindow());
            window.setVisible(true);
        });
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development\nServers";
    }
}
