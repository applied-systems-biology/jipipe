package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;

import java.util.List;

/**
 * A settings page that contains basic settings for all external environments.
 */
public interface ExternalEnvironmentSettings extends JIPipeParameterCollection {

    /**
     * Returns the presets.
     * This is an interface class for easy access
     *
     * @return the presets
     */
    List<JIPipeExternalEnvironment> getPresetsListInterface(Class<?> environmentClass);

    /**
     * Sets the presets
     * This is an interface class for easy access
     *
     * @param presets the presets
     */
    void setPresetsListInterface(List<JIPipeExternalEnvironment> presets, Class<?> environmentClass);

}
