package org.hkijena.jipipe.api.environments;

import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;

/**
 * A settings page that contains basic settings for all external environments
 * @param <T> The environment type
 * @param <U> A type that stores a list of the targeted environments
 */
public interface ExternalEnvironmentSettings<T extends ExternalEnvironment, U extends ListParameter<T>> extends JIPipeParameterCollection {
}
