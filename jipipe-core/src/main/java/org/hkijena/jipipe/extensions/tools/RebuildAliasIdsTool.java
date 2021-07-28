package org.hkijena.jipipe.extensions.tools;

import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.UIUtils;

public class RebuildAliasIdsTool extends JIPipeMenuExtension {
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

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Project";
    }
}
