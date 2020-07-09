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

package org.hkijena.acaq5.ui;

import org.hkijena.acaq5.ui.components.DocumentTabPane;
import org.scijava.Context;

import java.awt.*;

/**
 * Interface shared by all workbench UIs
 */
public interface ACAQWorkbench {

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
     * @return SciJava context
     */
    Context getContext();

    /**
     * @return The tab pane
     */
    DocumentTabPane getDocumentTabPane();
}
