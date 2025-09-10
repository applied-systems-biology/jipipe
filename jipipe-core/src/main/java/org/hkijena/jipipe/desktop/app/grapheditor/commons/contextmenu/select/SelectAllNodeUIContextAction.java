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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.select;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class SelectAllNodeUIContextAction implements GraphInteractiveObjectUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        return true;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        canvasUI.getSelectionManager().selectAll();
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
        return JIPipe.RESOURCES.getIcon16("actions/edit-select-all.png");
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
