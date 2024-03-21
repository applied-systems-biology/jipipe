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

import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.scijava.Context;

/**
 * Interface shared by all workbench UIs
 */
public interface JIPipeWorkbench {

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
     * Allows to send notifications on a workbench-level
     *
     * @return the notification inbox
     */
    JIPipeNotificationInbox getNotificationInbox();

    /**
     * Shows a message dialog
     *
     * @param message the message
     * @param title
     */
    void showMessageDialog(String message, String title);

    /**
     * Shows an error dialog
     *
     * @param message the message
     * @param title
     */
    void showErrorDialog(String message, String title);
}
