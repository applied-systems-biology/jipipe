package org.hkijena.jipipe.extensions.deeplearning.environments;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalStringParameter;
import org.hkijena.jipipe.extensions.python.installers.BasicMinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

@JIPipeDocumentation(name = "Download & install Tensorflow", description = "Creates a new Python environment with Tensorflow installed.")
@ExternalEnvironmentInfo(category = "Tensorflow")
public class TensorFlowEnvInstaller extends BasicMinicondaEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public TensorFlowEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setConfiguration(new Configuration());
//        getConfiguration().setCondaDownloadURL(getLatestPy37Download());
        getConfiguration().setInstallationPath(Paths.get("jipipe").resolve("tensorflow"));
        getConfiguration().setName("Tensorflow");
    }

    @Override
    public String getTaskLabel() {
        return "Install Tensorflow";
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        Configuration configuration = (Configuration) getConfiguration();

        if(configuration.isUsePip()) {
            Path environmentDefinitionPath = createPipRequirementsTxt();
            runConda("run", "--no-capture-output", "pip", "install", "-r", environmentDefinitionPath.toAbsolutePath().toString());
        }
        else {
            Path environmentDefinitionPath = createCondaEnvironmentYml();
            runConda("env", "update", "--file", environmentDefinitionPath.toAbsolutePath().toString());
        }

    }

    private Path createPipRequirementsTxt() {
        Path path = RuntimeSettings.generateTempFile("requirements", ".txt");
        // Create the appropriate environment
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            Configuration configuration = (Configuration) getConfiguration();
            String tensorFlowVersion = "";
            if(configuration.getTensorFlowVersion().isEnabled()) {
                tensorFlowVersion = "==" + configuration.getTensorFlowVersion();
            }
            for (String dependency : Arrays.asList(
                    "pyqt5",
                    "pandas",
                    "matplotlib",
                    "scipy",
                    "scikit-learn",
                    "scikit-image",
                    "opencv-python",
                    "tqdm",
                    "tifffile",
                    "tensorflow" + tensorFlowVersion,
                    "tensorflow-estimator" + tensorFlowVersion,
                    "keras>=2.3.1")) {
                writer.write(dependency);
                writer.newLine();
            }
            if (configuration.getH5pyVersion().isEnabled()) {
                writer.write("h5py");
                writer.write(configuration.getH5pyVersion().getContent());
                writer.newLine();
            } else {
                writer.write("h5py");
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    private Path createCondaEnvironmentYml() {
        Path path = RuntimeSettings.generateTempFile("environment", ".yml");
        // Create the appropriate environment
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            writer.write("name: base");
            writer.newLine();
            writer.write("dependencies:");
            writer.newLine();
            for (String dependency : Arrays.asList("python>3.5,<3.8",
                    "pyqt",
                    "pandas",
                    "matplotlib",
                    "scipy",
                    "scikit-learn",
                    "scikit-image",
                    "opencv",
                    "tqdm",
                    "tifffile",
                    "keras>=2.3.1")) {
                writer.write("  - ");
                writer.write(dependency);
                writer.newLine();
            }
            Configuration configuration = (Configuration) getConfiguration();
            if (configuration.getH5pyVersion().isEnabled()) {
                writer.write("  - h5py");
                writer.write(configuration.getH5pyVersion().getContent());
                writer.newLine();
            } else {
                writer.write("  - h5py");
                writer.newLine();
            }
            String tensorFlowVersion = "";
            if(configuration.getTensorFlowVersion().isEnabled()) {
                tensorFlowVersion = "==" + configuration.getTensorFlowVersion();
            }
            if (configuration.isWithGPU()) {
                writer.write("  - tensorflow-gpu" + tensorFlowVersion);
                writer.newLine();
            } else {
                writer.write("  - tensorflow" + tensorFlowVersion);
                writer.newLine();
            }
            // This is needed - otherwise conda might pull the wrong version
            writer.write("  - tensorflow-estimator" + tensorFlowVersion);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    public static class Configuration extends BasicMinicondaEnvPythonInstaller.Configuration {
        private boolean withGPU = true;
        private OptionalStringParameter tensorFlowVersion = new OptionalStringParameter("2.1.0", !SystemUtils.IS_OS_WINDOWS);
        private OptionalStringParameter h5pyVersion = new OptionalStringParameter("<3", !SystemUtils.IS_OS_WINDOWS);
        private boolean usePip = SystemUtils.IS_OS_WINDOWS;

        @JIPipeDocumentation(name = "GPU support", description = "If enabled, install Tensorflow with GPU support.")
        @JIPipeParameter("with-gpu")
        public boolean isWithGPU() {
            return withGPU;
        }

        @JIPipeParameter("with-gpu")
        public void setWithGPU(boolean withGPU) {
            this.withGPU = withGPU;
        }

        @JIPipeDocumentation(name = "Tensorflow version", description = "Determines the Tensorflow version. If you use GPU processing, " +
                "the Tensorflow version determines which CUDA toolkit version is installed. Please see https://www.tensorflow.org/install/source#gpu " +
                "for a table that indicates which CUDA version is supported by which Tensorflow version.\n\n" +
                "Info for Nvidia A100: Please install at least version 2.4.0, due to the requirement of cuda-toolkit >= 11.0")
        @JIPipeParameter("tensorflow-version")
        public OptionalStringParameter getTensorFlowVersion() {
            return tensorFlowVersion;
        }

        @JIPipeParameter("tensorflow-version")
        public void setTensorFlowVersion(OptionalStringParameter tensorFlowVersion) {
            this.tensorFlowVersion = tensorFlowVersion;
        }

        @JIPipeDocumentation(name = "h5py version", description = "If enabled, sets the version of h5py. There is a known bug with h5py versions >= 3.0 https://github.com/tensorflow/tensorflow/issues/44467")
        @JIPipeParameter("h5py-version")
        public OptionalStringParameter getH5pyVersion() {
            return h5pyVersion;
        }

        @JIPipeParameter("h5py-version")
        public void setH5pyVersion(OptionalStringParameter h5pyVersion) {
            this.h5pyVersion = h5pyVersion;
        }

        @JIPipeDocumentation(name = "Use pip", description = "Use pip instead of conda for setting up the environment. " +
                "This is usually not needed for Linux, but resolves DLL issues on Windows.")
        @JIPipeParameter("use-pip")
        public boolean isUsePip() {
            return usePip;
        }

        @JIPipeParameter("use-pip")
        public void setUsePip(boolean usePip) {
            this.usePip = usePip;
        }
    }
}
