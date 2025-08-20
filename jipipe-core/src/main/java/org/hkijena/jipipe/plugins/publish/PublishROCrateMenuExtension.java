package org.hkijena.jipipe.plugins.publish;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCratePublisherAssistant;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class PublishROCrateMenuExtension extends JIPipeDesktopMenuExtension implements ActionListener {

    /**
     * Creates a new instance
     *
     * @param desktopWorkbench workbench the extension is attached to
     */
    public PublishROCrateMenuExtension(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        setText("Create RO-Crate");
        setToolTipText("Publishes this project and all its inputs as RO-Crate with CWL.");
        setIcon(JIPipe.RESOURCES.getIcon16("apps/ro-crate.png"));
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
        getDesktopProjectWorkbench().getDocumentTabPane().addTab("Publish RO-Crate",
                JIPipe.RESOURCES.getIcon16("apps/ro-crate.png"),
                new ROCratePublisherAssistant(getDesktopProjectWorkbench()),
                JIPipeDesktopTabPane.CloseMode.withAskOnCloseButton);
        getDesktopProjectWorkbench().getDocumentTabPane().switchToLastTab();
    }
}
