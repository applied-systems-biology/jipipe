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

import org.hkijena.jipipe.desktop.app.grapheditor.commons.AbstractJIPipeDesktopGraphEditorUI;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class JIPipeConnectGraphEditorTool implements JIPipeToggleableGraphEditorTool {

    private AbstractJIPipeDesktopGraphEditorUI graphEditorUI;

    @Override
    public String getName() {
        return "Connect slots";
    }

    @Override
    public String getTooltip() {
        return "Only allows the connection of slots without moving nodes around";
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/lines-connector.png");
    }

    @Override
    public int getPriority() {
        return -9800;
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0);
    }

    @Override
    public AbstractJIPipeDesktopGraphEditorUI getGraphEditor() {
        return graphEditorUI;
    }

    @Override
    public void setGraphEditor(AbstractJIPipeDesktopGraphEditorUI graphEditorUI) {
        this.graphEditorUI = graphEditorUI;
    }

    @Override
    public void activate() {
    }

    @Override
    public int getCategory() {
        return -10000;
    }

    @Override
    public void deactivate() {

    }

    @Override
    public Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    @Override
    public boolean allowsDragNodes() {
        return false;
    }

    @Override
    public boolean allowsDragConnections() {
        return true;
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
