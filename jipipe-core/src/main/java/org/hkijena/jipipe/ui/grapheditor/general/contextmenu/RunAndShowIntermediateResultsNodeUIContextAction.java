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

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.grapheditor.algorithmpipeline.actions.RunAndShowResultsAction;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class RunAndShowIntermediateResultsNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if (selection.size() == 1) {
            JIPipeGraphNode node = selection.iterator().next().getNode();
            if (node instanceof JIPipeProjectCompartment)
                return true;
            if (!node.getInfo().isRunnable())
                return false;
            if (node.getParentGraph().getAttachment(JIPipeGraphType.class) != JIPipeGraphType.Project)
                return false;
            return true;
        }
        return false;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeNodeUI ui = selection.iterator().next();
        ui.getEventBus().post(new JIPipeGraphCanvasUI.NodeUIActionRequestedEvent(ui, new RunAndShowResultsAction(true)));
    }

    @Override
    public String getName() {
        return "Show intermediate results";
    }

    @Override
    public String getDescription() {
        return "Runs the pipeline up until this algorithm and shows the results (including intermediate results). " +
                "The results will be stored on the hard drive.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/rabbitvcs-update.png");
    }

    @Override
    public boolean disableOnNonMatch() {
        return false;
    }
}
