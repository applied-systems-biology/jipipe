package org.hkijena.jipipe.extensions.cellpose;

import org.apache.commons.exec.*;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.environments.PythonEnvironment;
import org.hkijena.jipipe.extensions.environments.installers.MinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.environments.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameters;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.MacroUtils;
import org.hkijena.jipipe.utils.WebUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Download & install Cellpose (CPU)", description = "Creates a new Python environment with Cellpose installed.")
public class CellPoseEnvInstaller extends MinicondaEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public CellPoseEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
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
        runConda("env", "create", "-f", environmentDefinitionPath.toAbsolutePath().toString());

        // Upgrade cellpose (pip)
        runConda("run", "--no-capture-output", "-n", "cellpose", "pip", "install", "cellpose", "--upgrade");
    }

    @Override
    protected SelectCondaEnvPythonInstaller.Configuration generateCondaConfig() {
        SelectCondaEnvPythonInstaller.Configuration configuration = super.generateCondaConfig();
        configuration.setEnvironmentName("cellpose");
        return configuration;
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
}
