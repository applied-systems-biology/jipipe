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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.io;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import javax.swing.*;
import java.util.Set;

public class EnablePassThroughNodeUIContextAction implements NodeUIContextAction {

    /**
     * The id of the shortcut action that triggers this context action
     * (see {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions}).
     */
    public static final String SHORTCUT_ACTION_ID = JIPipeShortcutActions.PASS_THROUGH_ENABLE.id();

    @Override
    public boolean matchesNodes(Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                if (!algorithm.isPassThrough() && algorithm.canPassThrough())
                    return true;
            }
        }
        return false;
    }

    @Override
    public void runNodes(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                if (algorithm.canPassThrough()) {
                    algorithm.setPassThrough(true);
                    algorithm.emitParameterChangedEvent("jipipe:algorithm:pass-through");
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Pass-through";
    }

    @Override
    public String getDescription() {
        return "Sets the selected algorithms to pass-though their input to their output without changes.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("emblems/pass-through.png");
    }

    @Override
    public String getShortcutActionId() {
        return SHORTCUT_ACTION_ID;
    }

    @Override
    public boolean isDisplayedInToolbar() {
        return true;
    }
}
