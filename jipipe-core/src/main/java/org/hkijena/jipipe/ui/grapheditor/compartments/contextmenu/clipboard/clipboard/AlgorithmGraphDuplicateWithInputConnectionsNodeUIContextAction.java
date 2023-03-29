/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeCompartmentOutput;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class AlgorithmGraphDuplicateWithInputConnectionsNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeGraph copyGraph = canvasUI.getGraph()
                .extract(selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet()), true);
        try {
            String json = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(copyGraph);
            Map<UUID, JIPipeGraphNode> pastedNodes = AlgorithmGraphPasteNodeUIContextAction.pasteNodes(canvasUI, json);

            // Reconnect to inputs
            for (Map.Entry<UUID, JIPipeGraphNode> entry : pastedNodes.entrySet()) {
                JIPipeGraphNode copyNode = entry.getValue();
                JIPipeGraphNode originalNode = canvasUI.getGraph().getNodeByUUID(entry.getKey());
                if (originalNode == null || originalNode instanceof JIPipeCompartmentOutput) {
                    continue;
                }
                for (JIPipeInputDataSlot originalInputSlot : originalNode.getInputSlots()) {
                    JIPipeInputDataSlot copyInputSlot = copyNode.getInputSlot(originalInputSlot.getName());
                    for (JIPipeDataSlot sourceSlot : canvasUI.getGraph().getInputIncomingSourceSlots(originalInputSlot)) {
                        if (!copyGraph.containsNode(sourceSlot.getNode().getUUIDInParentGraph())) {
                            canvasUI.getGraph().connect(sourceSlot, copyInputSlot);
                        }
                    }
                }
            }

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Duplicate (+ inputs)";
    }

    @Override
    public String getDescription() {
        return "Duplicates the selection. Any existing connections to the selected node(s) are preserved";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-duplicate.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK + InputEvent.SHIFT_MASK, true);
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }
}
