package org.hkijena.jipipe.plugins.publish;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PublishProjectReportMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener  {

    /**
     * Creates a new instance
     *
     * @param desktopWorkbench workbench the extension is attached to
     */
    public PublishProjectReportMenuExtension(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        setText("Generate project report");
        setToolTipText("Creates a text description of the project.");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/document-preview.png"));
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
        getDesktopProjectWorkbench().openProjectReport();
    }
}
