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

package org.hkijena.jipipe.plugins.cellpose.installers;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;

import java.nio.file.Paths;

@SetJIPipeDocumentation(name = "Miniconda: Download & install Cellpose (GPU) [deprecated]", description = "Creates a new Python environment with Cellpose installed. " +
        "Requires a graphics card suitable for GPU computing. Uses a Miniconda-based installer.")
@ExternalEnvironmentInfo(category = "Cellpose")
public class MinicondaCellposeGPUEnvInstaller extends MinicondaCellposeEnvInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public MinicondaCellposeGPUEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        this.setConfiguration(new Configuration());
        getConfiguration().setInstallationPath(Paths.get("jipipe").resolve("miniconda-cellpose-gpu"));
        getConfiguration().setName("Miniconda: Cellpose (GPU)");
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        // Uninstall torch
        runConda("run", "--no-capture-output", "pip", "uninstall", "--yes", "torch");

        // Install CUDA + pytorch
        getProgressInfo().log("Starting with GPU library installation. This will take a long time.");
        runConda("install",
                "--yes",
                "pytorch",
                "cudatoolkit=" + ((Configuration) getConfiguration()).getCudaToolkitVersion(),
                "-c",
                "pytorch",
                "--force-reinstall");
    }

    public static class Configuration extends MinicondaCellposeEnvInstaller.Configuration {
        private String cudaToolkitVersion = "10.2";

        @SetJIPipeDocumentation(name = "CUDA Toolkit version", description = "The version of the CUDA toolkit that should be " +
                "installed. The correct version depends on the operating system. Please see here: https://pytorch.org/get-started/locally/")
        @JIPipeParameter("cuda-toolkit-version")
        public String getCudaToolkitVersion() {
            return cudaToolkitVersion;
        }

        @JIPipeParameter("cuda-toolkit-version")
        public void setCudaToolkitVersion(String cudaToolkitVersion) {
            this.cudaToolkitVersion = cudaToolkitVersion;
        }
    }
}
