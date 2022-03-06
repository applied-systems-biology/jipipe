package org.hkijena.jipipe.extensions.cellpose;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.python.installers.BasicMinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.python.installers.BasicPythonEnvPythonInstaller;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.WebUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JIPipeDocumentation(name = "Python: Download & install Cellpose (CPU)", description = "Creates a new Python environment with Cellpose installed. " +
        "Uses a portable Python distribution. ")
@ExternalEnvironmentInfo(category = "Cellpose")
public class PythonCellPoseEnvInstaller extends BasicPythonEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public PythonCellPoseEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setConfiguration(new Configuration());
        getConfiguration().setInstallationPath(Paths.get("jipipe").resolve("python-cellpose-cpu"));
        getConfiguration().setName("Python: Cellpose (CPU)");
    }

    @Override
    public String getTaskLabel() {
        return "Install Cellpose";
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        getProgressInfo().log("Installing Cellpose");
        // We parse the environment provided by https://github.com/MouseLand/cellpose
        // It consists of pip packages anyway, so we parse them
        JsonNode environment = downloadEnvironment();
        List<String> pipArguments = new ArrayList<>();
        for (JsonNode item : ImmutableList.copyOf(environment.get("dependencies").elements())) {
            if(item.isObject() && item.has("pip")) {
                for (JsonNode pipItem : ImmutableList.copyOf(item.get("pip").elements())) {
                    pipArguments.add(pipItem.textValue());
                }
                break;
            }
        }
        getProgressInfo().log("Found following pip packages: " + String.join(", ", pipArguments));
        pipArguments.add(0, "install");
        runPip(pipArguments.toArray(new String[0]));
    }

    private JsonNode downloadEnvironment() {
        Path path = RuntimeSettings.generateTempFile("environment", ".yml");
        try {
            WebUtils.download(new URL("https://raw.githubusercontent.com/MouseLand/cellpose/master/environment.yml"),
                    path,
                    "Download environment",
                    getProgressInfo().resolveAndLog("Download environment.yml from MouseLand/cellpose"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readerFor(JsonNode.class).readValue(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Configuration extends BasicPythonEnvPythonInstaller.Configuration {
    }
}
