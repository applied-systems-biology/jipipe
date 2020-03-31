package org.hkijena.acaq5;

import io.scif.services.DatasetIOService;
import net.imagej.DatasetService;
import net.imagej.ops.OpService;
import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQRun;
import org.hkijena.acaq5.api.ACAQRunnerStatus;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.display.DisplayService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;

/**
 * Command that runs an ACAQ5 project without GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>ACAQ5>Run ACAQ5 project")
public class ACAQRunCommand implements Command {
    @Parameter
    private StatusService status;

    @Parameter
    private PluginService pluginService;

    @Parameter(label = "Project file (*.json)")
    private File parameterFile;

    @Parameter(label = "Output directory", style = "directory")
    private File outputDirectory;

    @Override
    public void run() {
        ACAQDefaultRegistry.instantiate(pluginService);
        try {
            ACAQProject project = ACAQProject.loadProject(parameterFile.toPath());
            ACAQMutableRunConfiguration configuration = new ACAQMutableRunConfiguration();
            configuration.setOutputPath(outputDirectory.toPath());
            ACAQRun run = new ACAQRun(project, configuration);
            run.run(this::onProgress, () -> false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onProgress(ACAQRunnerStatus runStatus) {
        status.showProgress(runStatus.getProgress(), runStatus.getMaxProgress());
        status.showStatus("ACAQ5: " + runStatus.getMessage());
    }
}
