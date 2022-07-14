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
 *
 */

package org.hkijena.jipipe.extensions.cellpose.installers;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.EasyInstallExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.cellpose.CellPoseSettings;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.util.List;

@JIPipeDocumentation(name = "Install Cellpose (EasyInstaller)", description = "Downloads a pre-packaged version of Cellpose")
@ExternalEnvironmentInfo(category = "Cellpose")
public class CellPoseEasyInstaller extends EasyInstallExternalEnvironmentInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public CellPoseEasyInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
    }

    @Override
    public String getTaskLabel() {
        return "Install Cellpose";
    }

    @Override
    public List<String> getRepositories() {
        return CellPoseSettings.getInstance().getEasyInstallerRepositories();
    }

    @Override
    public String getDialogHeading() {
        return "Install Cellpose";
    }

    @Override
    public HTMLText getDialogDescription() {
        return new HTMLText("Please select one of the pre-packaged versions of Cellpose. Please note that GPU acceleration is only supported by the GPU-capable packages. If you have issues, you can always try the version without CPU acceleration (CPU).");
    }

    @Override
    public ExternalEnvironment getInstalledEnvironment() {
        return null;
    }
}
