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

package org.hkijena.jipipe.ui.grapheditor.contextmenu.clipboard;

import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.history.CutNodeGraphHistorySnapshot;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.JIPipeNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;
import java.util.stream.Collectors;

public class AlgorithmGraphCutNodeUIContextAction extends AlgorithmGraphCopyNodeUIContextAction {

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeNodeUI> selection) {
        super.run(canvasUI, selection);
        Set<JIPipeGraphNode> nodes = selection.stream().map(JIPipeNodeUI::getNode).collect(Collectors.toSet());
        canvasUI.getGraphHistory().addSnapshotBefore(new CutNodeGraphHistorySnapshot(canvasUI.getGraph(), nodes));
        canvasUI.getGraph().removeNodes(nodes, true);
    }

    @Override
    public String getName() {
        return "Cut";
    }

    @Override
    public String getDescription() {
        return "Cuts the selection into the clipboard";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-cut.png");
    }

    @Override
    public boolean isShowingInOverhang() {
        return false;
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK, true);
    }
}
