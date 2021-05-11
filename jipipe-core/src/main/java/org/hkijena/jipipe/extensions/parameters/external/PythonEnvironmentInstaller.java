package org.hkijena.jipipe.extensions.parameters.external;

import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

/**
 * Contains instructions to install an environment.
 * Use {@link org.hkijena.jipipe.api.JIPipeDocumentation} to set the name and description for this installer
 */
public abstract class PythonEnvironmentInstaller implements JIPipeRunnable {

    private final JIPipeWorkbench workbench;
    private final JIPipeParameterAccess parameterAccess;

    /**
     *
     * @param workbench the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    protected PythonEnvironmentInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        this.workbench = workbench;
        this.parameterAccess = parameterAccess;
    }

    /**
     * Returns the final environment after installation
     * @return the final environment or null if there was an error
     */
    public abstract PythonEnvironmentParameter getInstalledEnvironment();

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public JIPipeParameterAccess getParameterAccess() {
        return parameterAccess;
    }
}
