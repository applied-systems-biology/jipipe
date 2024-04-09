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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.navigation;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.nodeui.JIPipeDesktopGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class MoveNodeUILeftContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeDesktopGraphNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphNodeUI> selection) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            Point point = ui.getStoredGridLocation();
            minX = Math.min(point.x, minX);
            minY = Math.min(point.y, minY);
        }
        canvasUI.expandLeftTop(minX <= 0 ? 1 : 0, 0);
        for (JIPipeDesktopGraphNodeUI ui : selection) {
            Point point = ui.getStoredGridLocation();
            ui.moveToGridLocation(new Point(point.x - 1, point.y), true, true);
        }
        canvasUI.repaint(50);
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String getName() {
        return "Move left";
    }

    @Override
    public String getDescription() {
        return "Move node(s) left";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/go-left.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, InputEvent.CTRL_MASK, true);
    }
}
