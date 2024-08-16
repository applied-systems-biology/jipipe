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

public class DisablePassThroughNodeUIContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            if (ui.getNode() instanceof JIPipeAlgorithm) {
                JIPipeAlgorithm algorithm = (JIPipeAlgorithm) ui.getNode();
                if (algorithm.isPassThrough())
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
                algorithm.setPassThrough(false);
                algorithm.emitParameterChangedEvent("jipipe:algorithm:pass-through");
            }
        }
    }

    @Override
    public String getName() {
        return "Disable pass-through";
    }

    @Override
    public String getDescription() {
        return "Sets the selected algorithms to process input to output.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("emblems/pass-through.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_MASK, true);
    }

    @Override
    public boolean isDisplayedInToolbar() {
        return true;
    }
}
