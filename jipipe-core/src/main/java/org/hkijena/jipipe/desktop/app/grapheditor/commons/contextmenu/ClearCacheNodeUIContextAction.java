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

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartmentOutput;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class ClearCacheNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI nodeUI : selection) {
            JIPipeGraphNode node = nodeUI.getNode();
            if (node.getParentGraph().getAttachment(JIPipeGraphType.class) != JIPipeGraphType.Project)
                return false;
        }
        for (JIPipeDesktopGraphNodeUI nodeUI : selection) {
            JIPipeGraphNode node = nodeUI.getNode();
            if (node instanceof JIPipeProjectCompartment)
                return true;
            if (node.getInfo().isRunnable())
                return true;
            if (node instanceof JIPipeAlgorithm)
                return true;
        }
        return false;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI nodeUI : selection) {
            JIPipeGraphNode node = nodeUI.getNode();
            JIPipeProject project = nodeUI.getGraphCanvasUI().getGraph().getProject();
            if (node instanceof JIPipeProjectCompartment) {
                for (JIPipeProjectCompartmentOutput output : ((JIPipeProjectCompartment) node).getOutputNodes().values()) {
                    project.getCache().clearAll(output.getUUIDInParentGraph(), false, new JIPipeProgressInfo());
                }

            } else {
                project.getCache().clearAll(node.getUUIDInParentGraph(), false, new JIPipeProgressInfo());
            }
        }
    }

    @Override
    public String getName() {
        return "Clear cache";
    }

    @Override
    public String getDescription() {
        return "Clears the cache of the selected node(s).";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/clear-brush.png");
    }

}
