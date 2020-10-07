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

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;

/**
 * An operation that is executed on showing existing data located in memory/cache.
 * This acts as additional entry in the cache browser display menu. Must be registered.
 */
public interface JIPipeDataDisplayOperation {
    /**
     * @return The name of this operation
     */
    String getName();

    /**
     * @return a description of the operation
     */
    String getDescription();

    /**
     * @return the order in menu. lower values are sorted to the top. The first one is used as default if the user did not select one.
     */
    int getOrder();

    /**
     * @return optional icon for the operation. can be null
     */
    Icon getIcon();

    /**
     * Shows the data in the UI
     * @param data the data
     * @param displayName
     * @param workbench the workbench that issued the command
     */
    void display(JIPipeData data, String displayName, JIPipeWorkbench workbench);
}
