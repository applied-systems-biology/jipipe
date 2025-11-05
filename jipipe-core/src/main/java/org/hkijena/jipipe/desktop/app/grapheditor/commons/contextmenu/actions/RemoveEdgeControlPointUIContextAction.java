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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeControlPointUI;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.Set;

public class RemoveEdgeControlPointUIContextAction implements GraphInteractiveObjectUIContextAction {

    @Override
    public String getName() {
        return "Remove control point";
    }

    @Override
    public String getDescription() {
        return "Removes the selected control points.";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/node-delete.png");
    }

    @Override
    public boolean matches(Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        return selection.stream().anyMatch(ui -> ui instanceof JIPipeDesktopGraphEdgeControlPointUI);
    }

    @Override
    public KeyStroke getKeyboardShortcut() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, true);
    }

    @Override
    public void run(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphInteractiveObjectUI> selection) {
        for (JIPipeDesktopGraphInteractiveObjectUI ui : selection) {
            if (ui instanceof JIPipeDesktopGraphEdgeControlPointUI edgeControlPointUI) {
                edgeControlPointUI.getEdgeUI().removeControlPoint(edgeControlPointUI);
            }
        }
    }

}
