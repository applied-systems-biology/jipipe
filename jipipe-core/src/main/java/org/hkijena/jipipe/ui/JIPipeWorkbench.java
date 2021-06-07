/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.scijava.Context;

import java.awt.*;

/**
 * Interface shared by all workbench UIs
 */
public interface JIPipeWorkbench {

    /**
     * Attempts to find a workbench
     *
     * @param graph  the graph
     * @param orElse if no workbench could be found
     * @return the workbench
     */
    static JIPipeWorkbench tryFindWorkbench(JIPipeGraph graph, JIPipeWorkbench orElse) {
        JIPipeProject project = graph.getAttachment(JIPipeProject.class);
        if (project != null) {
            JIPipeProjectWindow window = JIPipeProjectWindow.getWindowFor(project);
            if (window != null) {
                return window.getProjectUI();
            }
        }
        return orElse;
    }

    /**
     * Returns the window
     *
     * @return the window
     */
    Window getWindow();

    /**
     * Sends a text to the status bar
     *
     * @param text The text
     */
    void sendStatusBarText(String text);

    /**
     * Returns if the project is modified
     */
    boolean isProjectModified();

    /**
     * Sets the modification state of the project
     *
     * @param modified if the project is modified
     */
    void setProjectModified(boolean modified);

    /**
     * @return SciJava context
     */
    Context getContext();

    /**
     * @return The tab pane
     */
    DocumentTabPane getDocumentTabPane();
}
