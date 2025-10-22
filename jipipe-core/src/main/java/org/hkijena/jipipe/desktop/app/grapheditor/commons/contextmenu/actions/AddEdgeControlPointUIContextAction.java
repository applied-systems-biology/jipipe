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
import org.hkijena.jipipe.desktop.app.grapheditor.commons.canvas.managers.JIPipeDesktopGraphCanvasEdgeManager;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.EdgesOnlyUIContextAction;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.edgeui.JIPipeDesktopGraphEdgeUI;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class AddEdgeControlPointUIContextAction implements EdgesOnlyUIContextAction {

    @Override
    public String getName() {
        return "Split edge";
    }

    @Override
    public String getDescription() {
        return "Adds a control point ";
    }

    @Override
    public Icon getIcon() {
        return JIPipe.RESOURCES.getIcon16("actions/node-add.png");
    }

    @Override
    public boolean showInMultiSelectionPanel() {
        return false;
    }

    @Override
    public boolean matchesEdges(Set<JIPipeDesktopGraphEdgeUI> selection) {
        return !selection.isEmpty();
    }

    @Override
    public void runEdges(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeDesktopGraphEdgeUI> selection) {
        Point graphEditorCursor = canvasUI.getGraphEditorCursor();
        if (graphEditorCursor == null) {
            return;
        }

        JIPipeDesktopGraphCanvasEdgeManager edgeManager = canvasUI.getEdgeManager();
        for (JIPipeDesktopGraphEdgeUI edgeUI : selection) {
            edgeManager.addControlPointToEdge(edgeUI, graphEditorCursor);
        }
    }
}
