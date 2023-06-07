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

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class SelectAllNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        canvasUI.selectAll();
    }

    @Override
    public String getName() {
        return "Select all";
    }

    @Override
    public String getDescription() {
        return "Selects all nodes";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/edit-select-all.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK, true);
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }
}
