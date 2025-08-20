package org.hkijena.jipipe.plugins.publish;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PublishLegacyArchiveMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {

    /**
     * Creates a new instance
     *
     * @param desktopWorkbench workbench the extension is attached to
     */
    public PublishLegacyArchiveMenuExtension(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        setText("Create project archive (legacy)");
        setToolTipText("Creates a ZIP file or directory that contains all inputs of the current project. Please note that this is a legacy feature. We recommend to create a RO-Crate instead.");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/archive.png"));
        addActionListener(this);
    }


    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectPublishMenu;
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getDesktopProjectWorkbench().archiveProjectLegacy();
    }
}
