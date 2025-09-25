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

package org.hkijena.jipipe.api.settings;

import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.util.List;

/**
 * A settings sheet used for storing project-related settings
 */
public interface JIPipeProjectSettingsSheet extends JIPipeParameterCollection, JIPipeSettingsSheet {
    /**
     * Initialize this sheet during project initialization.
     * At this point, JIPipe's static wrappers are available.
     *
     * @param project the project
     */
    void initialize(JIPipeProject project);

    /**
     * Gathers all known external environments.
     * Environments should only be added if they are active/enabled.
     *
     * @param target the list where the external environments will be gathered
     */
    void getEnvironmentDependencies(List<JIPipeEnvironment> target);
}
