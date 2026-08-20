package org.hkijena.jipipe.launcher.commands;

import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializationSettings;
import org.hkijena.jipipe.api.service.JIPipeServiceMode;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.publish.rocrate.CreateROCrateRun;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateApplicationSettings;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateDockerSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateROCrateCommand {
    public static void doCreateROCrate(List<String> argsList) {
        JIPipeServiceInitializationSettings initializationSettings = new JIPipeServiceInitializationSettings();
        initializationSettings.setMode(JIPipeServiceMode.Headless);

        Path projectFile = null;
        Path outputFile = null;
        boolean verbose = false;
        boolean fastInit = false;
        Path overrideProfileDir = null;
        Map<String, JIPipeProjectUserPaths.Role> userPathOverrides = new HashMap<>();

        for (int i = 0; i < argsList.size(); i++) {
            String arg = argsList.get(i);
            boolean success = false;
            if (arg.equals("--fast-init")) {
                fastInit = true;
                success = true;
            } else if (arg.equals("--verbose")) {
                verbose = true;
                success = true;
            }
            if (success) {
                argsList.remove(i);
                --i;
            }
        }

        for (int i = 0; i < argsList.size(); i += 2) {
            String arg = argsList.get(i);
            String value = argsList.get(i + 1);
            if (arg.equals("--project")) {
                projectFile = Paths.get(value);
            } else if (arg.equals("--output")) {
                outputFile = Paths.get(value);
            } else if (arg.equals("--profile-dir")) {
                overrideProfileDir = Paths.get(value);
            } else if (arg.equals("--U")) {
                userPathOverrides.put(value, JIPipeProjectUserPaths.Role.Input);
            } else {
                System.err.println("Unknown argument: " + arg);
                HelpCommand.showHelp();
                return;
            }
        }

        if (projectFile == null || !Files.exists(projectFile)) {
            System.err.println("Project file does not exist!");
            HelpCommand.showHelp();
            return;
        }
        if (outputFile == null) {
            System.err.println("Output file not set!");
            HelpCommand.showHelp();
            return;
        }
        if (overrideProfileDir != null) {
            System.out.println("Overriding base path for JIPipe profiles with " + overrideProfileDir);
            PathUtils.createDirectories(overrideProfileDir);
            initializationSettings.setOverrideUserDirBase(overrideProfileDir);
        }

        initializationSettings.setVerbose(verbose);

        final ImageJ ij = new ImageJ();
        JIPipeService service = JIPipe.createInstance(ij.context(), initializationSettings);
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        extensionSettings.setSilent(true);
        if (fastInit) {
            extensionSettings.setValidateNodeTypes(false);
        }
        service.ensureInitialized();

        JIPipeValidationReport projectIssues = new JIPipeValidationReport();
        JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile, new UnspecifiedValidationReportContext(), projectIssues, notifications, JIPipeProgressInfo.STDOUT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, JIPipeProjectUserPaths.Role> entry : userPathOverrides.entrySet()) {
            System.out.println("Setting user path " + entry.getKey() + "=" + entry.getValue());
            project.getMetadata().getUserPaths().setUserPath(entry.getKey(), Paths.get(entry.getKey()));
        }

        projectIssues.print();

        ROCrateDockerSettings dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
        CreateROCrateRun run = new CreateROCrateRun(project, projectFile, outputFile, userPathOverrides, dockerSettings);
        run.setProgressInfo(JIPipeProgressInfo.STDOUT.resolveAndLog("Create RO-Crate"));
        run.run();

        try {
            project.close(JIPipeProgressInfo.STDOUT.resolve("Cleanup"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("RO-Crate created at " + outputFile);
    }
}
