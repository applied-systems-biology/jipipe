package org.hkijena.acaq5;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunSettings;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.hkijena.acaq5.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.acaq5.extensions.settings.RuntimeSettings;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import java.io.File;
import java.io.IOException;

/**
 * Command that runs an ACAQ5 project without GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>ACAQ5>Run ACAQ5 project")
public class ACAQRunCommand implements Command {

    @Parameter
    private Context context;

    @Parameter
    private StatusService status;

    @Parameter
    private PluginService pluginService;

    @Parameter(label = "Project file (*.json)")
    private File projectFile;

    @Parameter(label = "Output directory", style = "directory")
    private File outputDirectory;

    @Parameter(label = "Number of threads")
    private int threads = RuntimeSettings.getInstance().getDefaultRunThreads();

    @Override
    public void run() {
        ACAQDefaultRegistry.instantiate(context);
        ACAQProject project;
        try {
            project = ACAQProject.loadProject(projectFile.toPath());
            project.setWorkDirectory(projectFile.toPath().getParent());

        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not load project from '" + projectFile.toString() + "'!",
                    "Run ACAQ5 project", "Either the provided parameter file does not exist or is inaccessible, or it was corrupted.",
                    "Try to load the parameter file in the ACAQ5 GUI.");
        }

        ACAQRunSettings configuration = new ACAQRunSettings();
        configuration.setLoadFromCache(false);
        configuration.setStoreToCache(false);
        configuration.setOutputPath(outputDirectory.toPath());
        configuration.setNumThreads(threads);
        RuntimeSettings.getInstance().setDefaultRunThreads(threads);
        ACAQRun run = new ACAQRun(project, configuration);
        run.run(this::onProgress, () -> false);
    }

    private void onProgress(ACAQRunnerStatus runStatus) {
        status.showProgress(runStatus.getProgress(), runStatus.getMaxProgress());
        status.showStatus("ACAQ5: " + runStatus.getMessage());
    }
}
