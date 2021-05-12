package org.hkijena.jipipe.extensions.cellpose;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.python.installers.MinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.environments.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.WebUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@JIPipeDocumentation(name = "Download & install Cellpose (CPU)", description = "Creates a new Python environment with Cellpose installed.")
public class CellPoseEnvInstaller extends MinicondaEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public CellPoseEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setConfiguration(new Configuration());
        getConfiguration().setInstallationPath(Paths.get("cellpose-cpu"));
    }

    @Override
    public String getTaskLabel() {
        return "Install Cellpose";
    }

    @Override
    protected void postprocessInstall() {
        // We need to create the environment according to the CellPose tutorial https://github.com/MouseLand/cellpose
        Path environmentDefinitionPath = downloadEnvironment();

        // Apply the environment
        runConda("env", "update", "--file", environmentDefinitionPath.toAbsolutePath().toString());

        // Upgrade cellpose (pip)
        runConda("run", "--no-capture-output", "pip", "install", "cellpose", "--upgrade");

        // Download models
        if(((Configuration)getConfiguration()).isDownloadModels()) {
            runConda("run", "--no-capture-output", "python", "-u", "-c", "from cellpose import models");
        }
    }

    private Path downloadEnvironment() {
        Path path = RuntimeSettings.generateTempFile("environment", ".yml");
        try {
            WebUtils.download(new URL("https://raw.githubusercontent.com/MouseLand/cellpose/master/environment.yml"),
                    path,
                    "Download environment",
                    getProgressInfo().resolveAndLog("Download environment.yml from MouseLand/cellpose"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    public static class Configuration extends MinicondaEnvPythonInstaller.Configuration {
        private boolean downloadModels = true;

        @JIPipeDocumentation(name = "Download models", description = "If enabled, models will also be downloaded. " +
                "Otherwise, Cellpose might download the models during its first run.")
        @JIPipeParameter("download-models")
        public boolean isDownloadModels() {
            return downloadModels;
        }

        @JIPipeParameter("download-models")
        public void setDownloadModels(boolean downloadModels) {
            this.downloadModels = downloadModels;
        }
    }
}
