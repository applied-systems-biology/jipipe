package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Paths;

@JIPipeDocumentation(name = "Download & install Cellpose (GPU)", description = "Creates a new Python environment with Cellpose installed. " +
        "Requires a graphics card suitable for GPU computing.")
@ExternalEnvironmentInfo(category = "Cellpose")
public class CellPoseGPUEnvInstaller extends CellPoseEnvInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public CellPoseGPUEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        this.setConfiguration(new Configuration());
        getConfiguration().setInstallationPath(Paths.get("jipipe").resolve("cellpose-gpu"));
        getConfiguration().setName("Cellpose (GPU)");
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

    public static class Configuration extends CellPoseEnvInstaller.Configuration {
        private String cudaToolkitVersion = "10.2";

        @JIPipeDocumentation(name = "CUDA Toolkit version", description = "The version of the CUDA toolkit that should be " +
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
