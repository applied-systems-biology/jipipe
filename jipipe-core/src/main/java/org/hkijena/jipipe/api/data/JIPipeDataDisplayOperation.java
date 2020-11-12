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

/**
 * An operation that is executed on showing existing data located in memory/cache.
 * This acts as additional entry in the cache browser display menu. Must be registered.
 */
public interface JIPipeDataDisplayOperation extends JIPipeDataOperation {

    /**
     * Shows the data in the UI
     *
     * @param data        the data
     * @param displayName the display name
     * @param workbench   the workbench that issued the command
     * @param source      optional source of the data. Can by null or any kind of object (e.g. {@link JIPipeDataSlot})
     */
    void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source);
}
