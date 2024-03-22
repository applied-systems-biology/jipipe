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

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.run.JIPipeRunnable;

/**
 * Contains instructions to install an environment.
 * Use {@link SetJIPipeDocumentation} to set the name and description for this installer
 */
public abstract class JIPipeExternalEnvironmentInstaller implements JIPipeRunnable {

    private final JIPipeWorkbench workbench;
    private final JIPipeParameterAccess parameterAccess;

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public JIPipeExternalEnvironmentInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        this.workbench = workbench;
        this.parameterAccess = parameterAccess;
    }

    /**
     * Returns the final environment after installation
     *
     * @return the final environment or null if there was an error
     */
    public abstract JIPipeEnvironment getInstalledEnvironment();

    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }

    public JIPipeParameterAccess getParameterAccess() {
        return parameterAccess;
    }
}
