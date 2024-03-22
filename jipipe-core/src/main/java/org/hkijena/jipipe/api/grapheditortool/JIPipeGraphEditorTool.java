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

import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public interface JIPipeGraphEditorTool extends MouseMotionListener, MouseListener {

    String getName();

    String getTooltip();

    Icon getIcon();

    default int getPriority() {
        return 0;
    }

    default boolean supports(JIPipeDesktopGraphEditorUI graphEditorUI) {
        return true;
    }

    JIPipeDesktopGraphEditorUI getGraphEditor();

    void setGraphEditor(JIPipeDesktopGraphEditorUI graphEditorUI);

    default JIPipeDesktopGraphCanvasUI getGraphCanvas() {
        return getGraphEditor().getCanvasUI();
    }

    void activate();

    int getCategory();

    default Cursor getCursor() {
        return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    }

    default KeyStroke getKeyBinding() {
        return null;
    }
}
