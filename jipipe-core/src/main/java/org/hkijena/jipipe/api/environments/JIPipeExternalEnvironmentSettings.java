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

package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.util.List;

/**
 * A settings page that contains basic settings for all external environments.
 */
public interface JIPipeExternalEnvironmentSettings extends JIPipeParameterCollection {

    /**
     * Returns the presets.
     * This is an interface class for easy access
     *
     * @return the presets
     */
    List<JIPipeEnvironment> getPresetsListInterface(Class<?> environmentClass);

    /**
     * Sets the presets
     * This is an interface class for easy access
     *
     * @param presets the presets
     */
    void setPresetsListInterface(List<JIPipeEnvironment> presets, Class<?> environmentClass);

}
