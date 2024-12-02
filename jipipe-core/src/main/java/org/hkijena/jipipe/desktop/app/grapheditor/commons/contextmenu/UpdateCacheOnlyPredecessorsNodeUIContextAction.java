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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopUpdateCacheAction;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRun;
import org.hkijena.jipipe.desktop.app.quickrun.JIPipeDesktopQuickRunSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class UpdateCacheOnlyPredecessorsNodeUIContextAction implements NodeUIContextAction {
    private static void enqueue(JIPipeGraphNode node, JIPipeProject project) {
        JIPipeDesktopQuickRunSettings settings = new JIPipeDesktopQuickRunSettings(project);
        settings.setSaveToDisk(false);
        settings.setStoreToCache(true);
        settings.setStoreIntermediateResults(false);
        settings.setExcludeSelected(true);
        JIPipeDesktopQuickRun run = new JIPipeDesktopQuickRun(project, node, settings);
        JIPipeRunnableQueue.getInstance().enqueue(run);
    }

    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI nodeUI : selection) {
            JIPipeGraphNode node = nodeUI.getNode();
            if (node instanceof JIPipeProjectCompartment)
                return true;
            if (node.getParentGraph().getAttachment(JIPipeGraphType.class) != JIPipeGraphType.Project)
                continue;
            if (node.getInfo().isRunnable())
                return true;
            if (node instanceof JIPipeAlgorithm)
                return true;
        }
        return false;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        ImmutableList<JIPipeDesktopGraphNodeUI> list = ImmutableList.copyOf(selection);
        if (list.isEmpty()) {
            return;
        }

        // Batch mode (enqueue)
        for (int i = 0; i < list.size() - 1; i++) {
            JIPipeDesktopGraphNodeUI nodeUI = list.get(i);
            JIPipeGraphNode node = nodeUI.getNode();
            JIPipeProject project = node.getParentGraph().getProject();
            if (node instanceof JIPipeProjectCompartment) {
                for (JIPipeProjectCompartmentOutput output : ((JIPipeProjectCompartment) node).getOutputNodes().values()) {
                    enqueue(output, project);
                }
            } else if (node instanceof JIPipeAlgorithm || node.getInfo().isRunnable()) {
                enqueue(node, project);
            }
        }
        // Send last one to UI
        JIPipeDesktopGraphNodeUI ui = list.get(list.size() - 1);
        ui.getNodeUIActionRequestedEventEmitter().emit(new JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent(ui, new JIPipeDesktopUpdateCacheAction(false, true, true)));
    }

    @Override
    public String getName() {
        return "Update predecessor cache";
    }

    @Override
    public String getDescription() {
        return "Runs the pipeline up until the predecessors of the selected node. Nothing is written to disk.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/cache-predecessors.png");
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
