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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.UpdateCacheAction;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.ui.quickrun.QuickRun;
import org.hkijena.jipipe.ui.quickrun.QuickRunSettings;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class UpdateCacheNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        for (JIPipeGraphNodeUI nodeUI : selection) {
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
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        ImmutableList<JIPipeGraphNodeUI> list = ImmutableList.copyOf(selection);
        if(list.isEmpty()) {
            return;
        }

        // Batch mode (enqueue)
        for (int i = 0; i < list.size() - 1; i++) {
            JIPipeGraphNodeUI nodeUI = list.get(i);
            JIPipeGraphNode node = nodeUI.getNode();
            JIPipeProject project = node.getParentGraph().getProject();
            if (node instanceof JIPipeProjectCompartment) {
                node = ((JIPipeProjectCompartment) node).getOutputNode();
            }
            if (node instanceof JIPipeAlgorithm || node.getInfo().isRunnable()) {
                QuickRunSettings settings = new QuickRunSettings();
                settings.setSaveToDisk(false);
                settings.setStoreToCache(true);
                settings.setStoreIntermediateResults(false);
                settings.setExcludeSelected(false);
                QuickRun run = new QuickRun(project, node, settings);
                JIPipeRunnerQueue.getInstance().enqueue(run);
            }
        }
        // Send last one to UI
        JIPipeGraphNodeUI ui = list.get(list.size() - 1);
        ui.getNodeUIActionRequestedEventEmitter().emit(new JIPipeGraphNodeUI.NodeUIActionRequestedEvent(ui, new UpdateCacheAction(false, false)));
    }

    @Override
    public String getName() {
        return "Update cache";
    }

    @Override
    public String getDescription() {
        return "Runs the pipeline up until this algorithm and caches the results. Nothing is written to disk.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/database.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.CTRL_MASK, true);
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
