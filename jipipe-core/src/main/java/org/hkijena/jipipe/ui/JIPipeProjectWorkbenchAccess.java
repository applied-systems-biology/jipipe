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

import org.hkijena.jipipe.api.JIPipeProject;

/**
 * Panel that holds a reference to {@link JIPipeProjectWorkbench}
 */
public interface JIPipeProjectWorkbenchAccess extends JIPipeWorkbenchAccess {
    /**
     * @return The workbench UI
     */
    default JIPipeProjectWorkbench getProjectWorkbench() {
        return (JIPipeProjectWorkbench) getWorkbench();
    }

    /**
     * @return The project
     */
    default JIPipeProject getProject() {
        return getProjectWorkbench().getProject();
    }
}
