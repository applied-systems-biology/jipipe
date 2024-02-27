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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu;

import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.Set;

public class CollapseIOInterfaceNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return selection.stream().map(JIPipeGraphNodeUI::getNode).anyMatch(a -> a instanceof IOInterfaceAlgorithm && a.canUserDelete());
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        for (JIPipeGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof IOInterfaceAlgorithm) {
                IOInterfaceAlgorithm.collapse((IOInterfaceAlgorithm) ui.getNode());
            }
        }
    }

    @Override
    public String getName() {
        return "Collapse";
    }

    @Override
    public String getDescription() {
        return "Deletes the algorithm, but keeps the connections that were passed through it";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/delete.png");
    }

}
