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

import org.hkijena.jipipe.api.JIPipeGraphType;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.ui.grapheditor.actions.UpdateCacheAction;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class UpdateCacheNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeNodeUI> selection) {
        if (selection.size() == 1) {
            JIPipeGraphNode node = selection.iterator().next().getNode();
            if (!node.getInfo().isRunnable())
                return false;
            if (!(node instanceof JIPipeAlgorithm))
                return false;
            if (node.getGraph().getAttachment(JIPipeGraphType.class) != JIPipeGraphType.Project)
                return false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        JIPipeNodeUI ui = selection.iterator().next();
        ui.getEventBus().post(new JIPipeGraphCanvasUI.NodeUIActionRequestedEvent(ui, new UpdateCacheAction(false)));
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
