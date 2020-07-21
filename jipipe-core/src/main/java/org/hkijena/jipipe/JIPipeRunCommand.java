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
 */

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.JIPipeRun;
import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.JIPipeRunnerStatus;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.extensions.settings.RuntimeSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;

/**
 * Command that runs a JIPipe project without GUI
 */
@Plugin(type = Command.class, menuPath = "Plugins>JIPipe>Run JIPipe project")
public class JIPipeRunCommand implements Command {

    @Parameter
    private Context context;

    @Parameter
    private StatusService status;

    @Parameter
    private PluginService pluginService;

    @Parameter(label = "Project file (*.jip)")
    private File projectFile;

    @Parameter(label = "Output directory", style = "directory")
    private File outputDirectory;

    @Parameter(label = "Number of threads")
    private int threads = 1;

    @Override
    public void run() {
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        ExtensionSettings extensionSettings = ExtensionSettings.getInstanceFromRaw();
        JIPipeDefaultRegistry.instantiate(context, extensionSettings, issues);
        if(!extensionSettings.isSilent()) {
            JIPipeValidityReport report = new JIPipeValidityReport();
            issues.reportValidity(report);
            if (!report.isValid()) {
                if (GraphicsEnvironment.isHeadless()) {
                    report.print();
                } else {
                    UIUtils.openValidityReportDialog(null, report, false);
                }
            }
        }
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile.toPath());
            project.setWorkDirectory(projectFile.toPath().getParent());

        } catch (IOException e) {
            throw new UserFriendlyRuntimeException(e, "Could not load project from '" + projectFile.toString() + "'!",
                    "Run JIPipe project", "Either the provided parameter file does not exist or is inaccessible, or it was corrupted.",
                    "Try to load the parameter file in the JIPipe GUI.");
        }

        JIPipeRunSettings configuration = new JIPipeRunSettings();
        configuration.setLoadFromCache(false);
        configuration.setStoreToCache(false);
        configuration.setOutputPath(outputDirectory.toPath());
        configuration.setNumThreads(threads);
        RuntimeSettings.getInstance().setDefaultRunThreads(threads);
        JIPipeRun run = new JIPipeRun(project, configuration);
        run.run(this::onProgress, () -> false);
        System.out.println("JIPipe run finished. Outputs are stored in: " + outputDirectory);
        status.showProgress(0, 0);
    }

    private void onProgress(JIPipeRunnerStatus runStatus) {
        status.showProgress(runStatus.getProgress(), runStatus.getMaxProgress());
        status.showStatus("JIPipe: " + runStatus.getMessage());
        System.out.println("[" + runStatus.getProgress() + "/" + runStatus.getMaxProgress() + "] " + runStatus.getMessage());
    }
}
