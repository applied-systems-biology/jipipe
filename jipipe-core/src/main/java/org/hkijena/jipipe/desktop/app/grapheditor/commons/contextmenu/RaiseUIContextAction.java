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

import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class RaiseUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return selection.stream().anyMatch(ui -> !ui.getNode().isUiLocked() && ui.getNode() instanceof JIPipeAnnotationGraphNode);
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        canvasUI.raiseSelection(selection);
        canvasUI.repaintLowLag();
    }

    @Override
    public String getName() {
        return "Raise";
    }

    @Override
    public String getDescription() {
        return "Raises the Z of all selected graph annotations (functional and locked nodes are not affected)";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/object-order-raise.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, InputEvent.SHIFT_MASK);
    }

    @Override
    public boolean isDisplayedInToolbar() {
        return true;
    }
}
