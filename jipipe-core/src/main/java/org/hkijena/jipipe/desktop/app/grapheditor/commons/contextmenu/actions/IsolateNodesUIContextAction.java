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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.actions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import javax.swing.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class IsolateNodesUIContextAction implements NodeUIContextAction {

    /**
     * The id of the shortcut action that triggers this context action
     * (see {@link org.hkijena.jipipe.api.shortcuts.JIPipeShortcutActions}).
     */
    public static final String SHORTCUT_ACTION_ID = JIPipeShortcutActions.NODE_ISOLATE.id();

    @Override
    public boolean matchesNodes(Set<JIPipeDesktopGraphNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void runNodes(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        if (canvasUI.getHistoryJournal() != null) {
            Set<JIPipeGraphNode> nodes = selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).collect(Collectors.toSet());
            UUID compartment = nodes.stream().map(JIPipeGraphNode::getUUIDInParentGraph).findFirst().orElse(null);
            canvasUI.getHistoryJournal().snapshot("Isolate nodes", "Isolated nodes", compartment, JIPipe.RESOURCES.getIcon16("actions/network-disconnect.png"));
        }
        Set<JIPipeDataSlot> slots = new HashSet<>();
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            slots.addAll(ui.getNode().getInputSlots());
            slots.addAll(ui.getNode().getOutputSlots());
        }

        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : canvasUI.getGraph().getSlotEdges()) {
            boolean isSource = slots.contains(edge.getKey());
            boolean isTarget = slots.contains(edge.getValue());
            if (isSource != isTarget) {
                canvasUI.getGraph().disconnect(edge.getKey(), edge.getValue(), true);
            }
        }
    }

    @Override
    public String getName() {
        return "Isolate";
    }

    @Override
    public String getDescription() {
        return "Removes connections from and to the selected nodes, without removing internal connections.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/network-disconnect.png");
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
