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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.utils.PathUtils;
import org.scijava.Context;

import java.nio.file.Path;

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
     * @param title   title of the message
     */
    void showMessageDialog(String message, String title);

    /**
     * Shows an error dialog
     *
     * @param message the message
     * @param title   title of the message
     */
    void showErrorDialog(String message, String title);

    /**
     * Returns the current project if this workbench has an associated project
     *
     * @return the project or null
     */
    JIPipeProject getProject();

    /**
     * Creates a new temporary directory. Uses the project temporary directory as root if available.
     * @return the temporary directory
     */
    default Path newTempDirectory() {
        if(getProject() != null) {
            return getProject().newTemporaryDirectory();
        }
        else {
            return PathUtils.createGlobalTempDirectory("");
        }
    }

    /**
     * Creates a new temporary directory. Uses the project temporary directory as root if available.
     * @param baseName the base name
     * @return the temporary directory
     */
    default Path newTempDirectory(String baseName) {
        if(getProject() != null) {
            return getProject().newTemporaryDirectory(baseName);
        }
        else {
            return PathUtils.createGlobalTempDirectory(baseName);
        }
    }

    /**
     * Creates a new temporary file path. Uses the project temporary directory as root if available.
     * @param baseName the base name
     * @param suffix the suffix (extensions must include the dot)
     * @return path to a non-existing temporary file
     */
    default Path newTemporaryFilePath(String baseName, String suffix) {
        if(getProject() != null) {
            return getProject().newTemporaryFilePath(baseName, suffix);
        }
        else {
            return PathUtils.createGlobalTempFilePath(baseName, suffix);
        }
    }
}
