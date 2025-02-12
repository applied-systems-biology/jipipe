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

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.plugins.settings.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Context;
import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;

import java.awt.*;
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

    @Parameter
    private LogService logService;

    @Parameter(label = "Project file (*.jip)")
    private File projectFile;

    @Parameter(label = "Output directory", style = "directory")
    private File outputDirectory;

    @Parameter(label = "Number of threads")
    private int threads = 1;

    @Override
    public void run() {
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        if (JIPipe.getInstance() == null) {
            JIPipe jiPipe = JIPipe.createInstance(context);
            jiPipe.initialize(extensionSettings, issues, true);
            JIPipe.getInstance().initialize(extensionSettings, issues, true);
        }
        if (!extensionSettings.isSilent()) {
            JIPipeValidationReport report = new JIPipeValidationReport();
            issues.reportValidity(new UnspecifiedValidationReportContext(), report);
            if (!report.isEmpty()) {
                if (GraphicsEnvironment.isHeadless()) {
                    report.print();
                } else {
                    UIUtils.showValidityReportDialog(new JIPipeDesktopDummyWorkbench(),
                            null,
                            report,
                            "Errors while initializing JIPipe",
                            "There were some issues while initializing JIPipe. Please run the JIPipe GUI for more information.",
                            false);
                }
            }
        }
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile.toPath(), new UnspecifiedValidationReportContext(), new JIPipeValidationReport(), new JIPipeNotificationInbox());
            project.setWorkDirectory(projectFile.toPath().getParent());

        } catch (IOException e) {
            throw new JIPipeValidationRuntimeException(
                    e,
                    "Could not load project from '" + projectFile.toString() + "'!",
                    "Either the provided parameter file does not exist or is inaccessible, or it was corrupted.",
                    "Try to load the parameter file in the JIPipe GUI.");
        }

        JIPipeGraphRunConfiguration configuration = new JIPipeGraphRunConfiguration();
        configuration.setLoadFromCache(false);
        configuration.setStoreToCache(false);
        configuration.setOutputPath(outputDirectory.toPath());
        configuration.setNumThreads(threads);
        JIPipeRuntimeApplicationSettings.getInstance().setDefaultRunThreads(threads);
        JIPipeGraphRun run = new JIPipeGraphRun(project, configuration);
        run.run();
        logService.info("JIPipe run finished. Outputs are stored in: " + outputDirectory);
        status.showProgress(0, 0);
    }
}
