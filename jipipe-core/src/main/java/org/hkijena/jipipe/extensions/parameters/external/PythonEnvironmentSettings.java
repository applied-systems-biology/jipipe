package org.hkijena.jipipe.extensions.parameters.external;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings for a {@link PythonEnvironmentParameter}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PythonEnvironmentSettings {
    /**
     * Returns all available installers for this environment parameter
     * @return the installers
     */
    Class<? extends PythonEnvironmentInstaller>[] installers();
}
