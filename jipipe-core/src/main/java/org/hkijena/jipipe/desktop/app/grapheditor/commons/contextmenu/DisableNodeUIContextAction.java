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

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class DisableNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                if (algorithm.isEnabled())
                    return true;
            }
        }
        return false;
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                algorithm.setEnabled(false);
                algorithm.emitParameterChangedEvent("jipipe:algorithm:enabled");
            }
        }
    }

    @Override
    public String getName() {
        return "Disable";
    }

    @Override
    public String getDescription() {
        return "Disables the selected algorithms";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("emblems/block.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK, true);
    }

    @Override
    public boolean isDisplayedInToolbar() {
        return true;
    }
}
