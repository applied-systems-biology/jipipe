package org.hkijena.jipipe.extensions.nodetoolboxtool;

import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.event.ActionListener;

@JIPipeOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu)
public class NodeToolBoxMenuExtension extends MenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public NodeToolBoxMenuExtension(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Node tool box");
        setToolTipText("Alternative method of adding new nodes via drag & drop");
        setIcon(UIUtils.getIconFromResources("actions/cm_thumbnailsview.png"));
        addActionListener(e -> {
            NodeToolBoxWindow.openNewToolBox();
        });
    }

}
