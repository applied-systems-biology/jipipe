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

package org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.actions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphInteractiveObjectUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasEdgeManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasNotificationsManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.EdgesOnlyUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Set;

public class ClearEdgeControlsPointUIContextAction implements GraphInteractiveObjectUIContextAction {

    @Override
    public boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        return selection.stream().anyMatch(ui -> {
            if(ui instanceof JIPipeDesktopGraphEdgeUI edgeUI) {
                return !edgeUI.getControlPoints().isEmpty();
            }
            return false;
        });
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        JIPipeDesktopGraphCanvasEdgeManager edgeManager = canvasUI.getEdgeManager();
        for (JIPipeDesktopGraphInteractiveObjectUI ui : selection) {
            if(ui instanceof JIPipeDesktopGraphEdgeUI edgeUI) {
                edgeManager.clearControlPoints(edgeUI);
            }
        }
        canvasUI.getNotificationsManager().addNotification("Control points cleared",
                JIPipe.RESOURCES.getIcon16("actions/format-remove-node.png"),
                JIPipeDesktopGraphCanvasNotificationsManager.NotificationType.Success);
    }

    @Override
    public String getName() {
        return "Clear control points";
    }

    @Override
    public String getDescription() {
        return "Removes all control points within the selected edges";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/format-remove-node.png");
    }


     @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, KeyEvent.SHIFT_DOWN_MASK, true);
    }
}
