package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Python: Download & install Cellpose (GPU)", description = "Creates a new Python environment with Cellpose installed. " +
        "Requires a graphics card suitable for GPU computing. Uses a portable Python distribution.")
@ExternalEnvironmentInfo(category = "Cellpose")
public class PythonCellPoseGPUEnvInstaller extends PythonCellPoseEnvInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public PythonCellPoseGPUEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        this.setConfiguration(new Configuration());
        getConfiguration().setInstallationPath(Paths.get("jipipe").resolve("python-cellpose-gpu"));
        getConfiguration().setName("Python: Cellpose (GPU)");
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        // Install CUDA + pytorch
        getProgressInfo().log("Starting with GPU library installation. This will take a long time.");
        List<String> args = new ArrayList<>();
        args.add("install");
        args.addAll(Arrays.asList(((Configuration) getConfiguration()).getPyTorchPipPackage().split(" ")));
        runPip(args.toArray(new String[0]));
    }

    public static class Configuration extends PythonCellPoseEnvInstaller.Configuration {
       private String pyTorchPipPackage = "torch==1.10.2+cu102 torchvision==0.11.3+cu102 torchaudio===0.10.2+cu102 -f https://download.pytorch.org/whl/cu102/torch_stable.html";

       @JIPipeDocumentation(name = "PyTorch pip package", description = "The pip package that is used for installing PyTorch. " +
               "To generate custom settings, visit https://pytorch.org/get-started/locally/ and copy the parts after 'pip3 install' into this field.")
       @JIPipeParameter("pytorch-pip-package")
       @StringParameterSettings(monospace = true)
        public String getPyTorchPipPackage() {
            return pyTorchPipPackage;
        }

        @JIPipeParameter("pytorch-pip-package")
        public void setPyTorchPipPackage(String pyTorchPipPackage) {
            this.pyTorchPipPackage = pyTorchPipPackage;
        }
    }
}
