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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.misc;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.compartments.algorithms.IOInterfaceAlgorithm;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;

import javax.swing.*;
import java.util.Set;

public class CollapseIOInterfaceNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matchesNodes(Set<JIPipeDesktopGraphNodeUI> selection) {
        return selection.stream().map(JIPipeDesktopGraphNodeUI::getNode).anyMatch(a -> a instanceof IOInterfaceAlgorithm && a.canUserDelete());
    }

    @Override
    public void runNodes(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
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
        return JIPipe.RESOURCES.getIcon16("actions/delete.png");
    }

}
