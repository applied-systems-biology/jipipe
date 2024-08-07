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
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.desktop.app.grapheditor.flavors.pipeline.actions.JIPipeDesktopRunAndShowResultsAction;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class RunAndShowResultsNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
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
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        JIPipeDesktopGraphNodeUI ui = selection.iterator().next();
        ui.getNodeUIActionRequestedEventEmitter().emit(new JIPipeDesktopGraphNodeUI.NodeUIActionRequestedEvent(ui, new JIPipeDesktopRunAndShowResultsAction(false)));
    }

    @Override
    public String getName() {
        return "Run & show results";
    }

    @Override
    public String getDescription() {
        return "Runs the pipeline up until this algorithm and shows the results. " +
                "The results will be stored on the hard drive.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/media-play.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK, true);
    }

}
