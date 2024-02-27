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

package org.hkijena.jipipe.ui.extension;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;

import javax.swing.*;

public abstract class GraphEditorToolBarButtonExtension extends JButton {
    private final JIPipeGraphEditorUI graphEditorUI;

    /**
     * Creates a new instance
     *
     * @param graphEditorUI the graph editor
     */
    public GraphEditorToolBarButtonExtension(JIPipeGraphEditorUI graphEditorUI) {
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
        return graphEditorUI.getWorkbench();
    }

    public JIPipeGraphEditorUI getGraphEditorUI() {
        return graphEditorUI;
    }
}
