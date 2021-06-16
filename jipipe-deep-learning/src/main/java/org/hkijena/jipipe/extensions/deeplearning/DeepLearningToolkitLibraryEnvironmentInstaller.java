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

package org.hkijena.jipipe.extensions.deeplearning;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.python.PythonPackageLibraryEnvironmentInstaller;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@JIPipeDocumentation(name = "Reinstall", description = "Reinstall the library from the version provided within the plugin")
public class DeepLearningToolkitLibraryEnvironmentInstaller extends PythonPackageLibraryEnvironmentInstaller<DeepLearningToolkitLibraryEnvironment> {

    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public DeepLearningToolkitLibraryEnvironmentInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    protected DeepLearningToolkitLibraryEnvironment createEnvironment() {
        return new DeepLearningToolkitLibraryEnvironment();
    }

    @Override
    protected String getEnvironmentName() {
        return "Deep Learning Toolkit";
    }
}