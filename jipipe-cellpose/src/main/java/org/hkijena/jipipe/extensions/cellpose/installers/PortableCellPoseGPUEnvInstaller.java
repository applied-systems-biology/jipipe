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

import org.apache.commons.lang3.SystemUtils;
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
public class PortableCellPoseGPUEnvInstaller extends PortableCellPoseEnvInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public PortableCellPoseGPUEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
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

    public static class Configuration extends PortableCellPoseEnvInstaller.Configuration {
        private String pyTorchPipPackage;

        public Configuration() {
            if(SystemUtils.IS_OS_MAC_OSX) {
                pyTorchPipPackage = "torch torchvision torchaudio";
            }
            else {
                pyTorchPipPackage = "torch==1.10.2+cu102 torchvision==0.11.3+cu102 torchaudio===0.10.2+cu102 -f https://download.pytorch.org/whl/cu102/torch_stable.html";
            }
        }

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
