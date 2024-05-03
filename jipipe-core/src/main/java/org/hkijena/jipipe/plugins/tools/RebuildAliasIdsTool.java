/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.tools;

import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

public class RebuildAliasIdsTool extends JIPipeDesktopMenuExtension {
    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public RebuildAliasIdsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Force rebuild alias IDs");
        setToolTipText("Rebuilds the node alias IDs for all nodes. This can help if the " +
                "generated alias IDs are too long.");
        setIcon(UIUtils.getIconFromResources("actions/tag.png"));
        addActionListener(e -> rebuildIds());
    }

    private void rebuildIds() {
        JIPipeDesktopProjectWorkbench workbench = (JIPipeDesktopProjectWorkbench) getDesktopWorkbench();
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
