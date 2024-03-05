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

package org.hkijena.jipipe.ui.grapheditor.compartments.contextmenu.clipboard.clipboard;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class AlgorithmGraphPasteNodeUIContextAction implements NodeUIContextAction {

    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        try {
            String json = UIUtils.getStringFromClipboard();
            if (json != null) {
                canvasUI.pasteNodes(json);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(canvasUI.getWorkbench().getWindow(), "The current clipboard contents are no valid nodes/graph.", "Paste nodes", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Paste";
    }

    @Override
    public String getDescription() {
        return "Copies nodes from clipboard into the current graph";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-paste.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK, true);
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }
}
