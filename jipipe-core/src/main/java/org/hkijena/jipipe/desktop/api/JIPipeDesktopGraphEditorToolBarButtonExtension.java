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

package org.hkijena.jipipe.desktop.api;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphEditorUI;

import javax.swing.*;

public abstract class JIPipeDesktopGraphEditorToolBarButtonExtension extends JButton {
    private final JIPipeDesktopGraphEditorUI graphEditorUI;

    /**
     * Creates a new instance
     *
     * @param graphEditorUI the graph editor
     */
    public JIPipeDesktopGraphEditorToolBarButtonExtension(JIPipeDesktopGraphEditorUI graphEditorUI) {
        this.graphEditorUI = graphEditorUI;
    }

    /**
     * Override this method to determine if this button should be available for a graph
     *
     * @return if the button is displayed
     */
    public boolean isVisibleInGraph() {
        return true;
    }

    public JIPipeWorkbench getWorkbench() {
        return graphEditorUI.getDesktopWorkbench();
    }

    public JIPipeDesktopGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }
}
