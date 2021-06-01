package org.hkijena.jipipe.extensions.tools;

import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.utils.UIUtils;

@JIPipeOrganization(menuExtensionTarget = MenuTarget.ProjectToolsMenu, menuPath = "Project")
public class RebuildAliasIdsTool extends MenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public RebuildAliasIdsTool(JIPipeWorkbench workbench) {
        super(workbench);
        setText("Force rebuild alias IDs");
        setToolTipText("Rebuilds the node alias IDs for all nodes. This can help if the " +
                "generated alias IDs are too long.");
        setIcon(UIUtils.getIconFromResources("actions/tag.png"));
        addActionListener(e -> rebuildIds());
    }

    private void rebuildIds() {
        JIPipeProjectWorkbench workbench = (JIPipeProjectWorkbench) getWorkbench();
        workbench.getProject().rebuildAliasIds(true);
        workbench.sendStatusBarText("Rebuilt alias IDs");
    }
}
