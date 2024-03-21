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

package org.hkijena.jipipe.ui;

import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;

import java.awt.*;

/**
 * A user interface that is running on a desktop machine
 */
public interface JIPipeDesktopWorkbench extends JIPipeWorkbench {
    /**
     * @return The tab pane
     */
    DocumentTabPane getDocumentTabPane();

    /**
     * Returns the window
     *
     * @return the window
     */
    Window getWindow();
}
