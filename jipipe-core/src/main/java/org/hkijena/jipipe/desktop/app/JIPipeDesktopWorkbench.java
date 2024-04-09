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

package org.hkijena.jipipe.desktop.app;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.commons.components.tabs.JIPipeDesktopTabPane;

import java.awt.*;

/**
 * A user interface that is running on a desktop machine
 */
public interface JIPipeDesktopWorkbench extends JIPipeWorkbench {
    /**
     * @return The tab pane
     */
    JIPipeDesktopTabPane getDocumentTabPane();

    /**
     * Returns the window
     *
     * @return the window
     */
    Window getWindow();

    /**
     * Method that returns whether the underlying project was modified
     *
     * @return if the project was modified
     */
    boolean isProjectModified();

    /**
     * Marks/unmarks the project of this workbench as modified
     *
     * @param oldModified the status
     */
    void setProjectModified(boolean oldModified);
}
