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

package org.hkijena.jipipe.api.grapheditortool;

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class JIPipeCropViewGraphEditorTool implements JIPipeGraphEditorTool {

    private JIPipeDesktopGraphEditorUI graphEditor;

    @Override
    public String getName() {
        return "Center view to nodes";
    }

    @Override
    public String getTooltip() {
        return "Removes large empty areas around the graph.";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/view-restore.png");
    }

    @Override
    public JIPipeDesktopGraphEditorUI getGraphEditor() {
        return graphEditor;
    }

    @Override
    public void setGraphEditor(JIPipeDesktopGraphEditorUI graphEditorUI) {
        this.graphEditor = graphEditorUI;
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);
    }

    @Override
    public void activate() {
        if (graphEditor.getHistoryJournal() != null) {
            graphEditor.getHistoryJournal().snapshot("Center view to nodes",
                    "Apply center view to nodes",
                    graphEditor.getCompartment(),
                    UIUtils.getIconFromResources("actions/view-restore.png"));
        }
        graphEditor.getCanvasUI().crop(true);
    }

    @Override
    public int getCategory() {
        return 10000;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {

    }
}
