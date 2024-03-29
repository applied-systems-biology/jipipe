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

package org.hkijena.jipipe.extensions.cellpose.installers;

import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.python.installers.BasicMinicondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.WebUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@SetJIPipeDocumentation(name = "Miniconda: Download & install Cellpose (CPU) [deprecated]", description = "Creates a new Python environment with Cellpose installed. " +
        "Uses a Miniconda-based installer. ")
@ExternalEnvironmentInfo(category = "Cellpose")
public class MinicondaCellposeEnvInstaller extends BasicMinicondaEnvPythonInstaller {
    /**
     * @param workbench       the workbench
     * @param parameterAccess the parameter access that will receive the generated environment
     */
    public MinicondaCellposeEnvInstaller(JIPipeWorkbench workbench, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterAccess);
        setConfiguration(new Configuration());
        getConfiguration().setInstallationPath(Paths.get("jipipe").resolve("miniconda-cellpose-cpu"));
        getConfiguration().setName("Miniconda: Cellpose (CPU)");
    }

    @Override
    public String getTaskLabel() {
        return "Install Cellpose";
    }

    @Override
    protected void postprocessInstall() {
        super.postprocessInstall();

        // We need to create the environment according to the Cellpose tutorial https://github.com/MouseLand/cellpose
        Path environmentDefinitionPath = downloadEnvironment();

        // Apply the environment
        runConda("env", "update", "--file", environmentDefinitionPath.toAbsolutePath().toString());

        // Upgrade cellpose (pip)
        runConda("run", "--no-capture-output", "pip", "install", "cellpose", "--upgrade");
        runConda("run", "--no-capture-output", "pip", "install", "cellpose[gui]");

        // Download models
//        if (((Configuration) getConfiguration()).isDownloadModels()) {
//            try {
//                runConda("run", "--no-capture-output", "python", "-u", "-c", "from cellpose import models; models.download_model_weights()");
//            }
//            catch (Exception e) {
//                getProgressInfo().log("Unable to download models! (You can ignore this, Cellpose will download models on the first start)");
//            }
//        }
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
        getProgressInfo().log("Renaming 'cellpose' to 'base' due to bug in conda run and conda env");
        try {
            String contents = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            contents = contents.replace("name: cellpose", "name: base");
            Files.write(path, contents.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return path;
    }

    public static class Configuration extends BasicMinicondaEnvPythonInstaller.Configuration {
//        private boolean downloadModels = true;
//
//        @JIPipeDocumentation(name = "Download models", description = "If enabled, models will also be downloaded. " +
//                "Otherwise, Cellpose might download the models during its first run.")
//        @JIPipeParameter("download-models")
//        public boolean isDownloadModels() {
//            return downloadModels;
//        }
//
//        @JIPipeParameter("download-models")
//        public void setDownloadModels(boolean downloadModels) {
//            this.downloadModels = downloadModels;
//        }
    }
}
