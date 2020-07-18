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

package org.hkijena.jipipe.ui.grapheditor.contextmenu;

import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.algorithm.JIPipeNodeCategory;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.history.GraphChangedHistorySnapshot;
import org.hkijena.jipipe.api.history.RemoveNodeGraphHistorySnapshot;
import org.hkijena.jipipe.extensions.settings.GraphEditorUISettings;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IsolateNodesUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if(selection.isEmpty())
            return false;
        for (JIPipeNodeUI ui : selection) {
            if(ui.getNode().getCategory() == JIPipeNodeCategory.Internal)
                return false;
        }
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        canvasUI.getGraphHistory().addSnapshotBefore(new GraphChangedHistorySnapshot(canvasUI.getGraph(), "Isolated nodes"));
        Set<JIPipeDataSlot> slots = new HashSet<>();
        for (JIPipeNodeUI ui : selection) {
            slots.addAll(ui.getNode().getInputSlots());
            slots.addAll(ui.getNode().getOutputSlots());
        }

        for (Map.Entry<JIPipeDataSlot, JIPipeDataSlot> edge : canvasUI.getGraph().getSlotEdges()) {
            boolean isSource = slots.contains(edge.getKey());
            boolean isTarget = slots.contains(edge.getValue());
            if(isSource != isTarget) {
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
        return UIUtils.getIconFromResources("connect.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return true;
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
