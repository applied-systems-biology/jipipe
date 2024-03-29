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

package org.hkijena.jipipe.ui.grapheditor.general.contextmenu.navigation;

import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphCanvasUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.grapheditor.general.nodeui.JIPipeGraphNodeUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

public class MoveNodeUIUpContextAction implements NodeUIContextAction {
    @Override
    public boolean matches(Set<JIPipeGraphNodeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void run(JIPipeGraphCanvasUI canvasUI, Set<JIPipeGraphNodeUI> selection) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (JIPipeGraphNodeUI ui : selection) {
            Point point = ui.getStoredGridLocation();
            minX = Math.min(point.x, minX);
            minY = Math.min(point.y, minY);
        }
        canvasUI.expandLeftTop(0, minY <= 0 ? 1 : 0);
        for (JIPipeGraphNodeUI ui : selection) {
            Point point = ui.getStoredGridLocation();
            ui.moveToGridLocation(new Point(point.x, point.y - 1), true, true);
        }
        canvasUI.repaint(50);
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String getName() {
        return "Move up";
    }

    @Override
    public String getDescription() {
        return "Move node(s) up";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/go-up.png");
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_MASK, true);
    }
}
