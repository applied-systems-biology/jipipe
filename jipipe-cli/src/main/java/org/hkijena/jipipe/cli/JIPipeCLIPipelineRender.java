package org.hkijena.jipipe.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import net.imagej.ImageJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeRegistryIssues;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.history.JIPipeDedicatedGraphHistoryJournal;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.plugins.settings.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JIPipeCLIPipelineRender {
    static void doRenderPipeline(List<String> argsList) {
        Path projectFile = null;
        Path outputFile = null;
        String compartmentNameOrUUID = null;

        boolean verbose = false;
        boolean fastInit = false;
        Path overrideProfileDir = null;

        // Parse flags
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

            // Delete the flag
            if (success) {
                argsList.remove(i);
                --i;
            }
        }

        // Parse key value pairs
        for (int i = 0; i < argsList.size(); i += 2) {
            String arg = argsList.get(i);
            String value = argsList.get(i + 1);
            if (arg.equals("--project")) {
                projectFile = Paths.get(value);
            } else if (arg.equals("--output")) {
                outputFile = Paths.get(value);
            } else if (arg.equals("--compartment")) {
                compartmentNameOrUUID = value;
            } else {
                System.err.println("Unknown argument: " + arg);
                JIPipeCLIHelp.showHelp();
                return;
            }
        }

        if (projectFile == null || !Files.exists(projectFile)) {
            System.err.println("Project file does not exist!");
            JIPipeCLIHelp.showHelp();
            return;
        }
        if(outputFile == null) {
            throw new NullPointerException("Output file not set!");
        }

        final ImageJ ij = new ImageJ();
        JIPipe jiPipe = JIPipe.createInstance(ij.context());
        JIPipeExtensionApplicationSettings extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        extensionSettings.setSilent(true);
        if (fastInit) {
            extensionSettings.setValidateNodeTypes(false);
        }
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        jiPipe.initialize(extensionSettings, issues, verbose);

        JIPipeValidationReport projectIssues = new JIPipeValidationReport();
        JIPipeNotificationInbox notifications = new JIPipeNotificationInbox();
        JIPipeProject project;
        try {
            project = JIPipeProject.loadProject(projectFile, new UnspecifiedValidationReportContext(), projectIssues, notifications);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JIPipeProjectCompartment targetCompartment = null;
        for (Map.Entry<UUID, JIPipeProjectCompartment> entry : project.getCompartments().entrySet()) {
            if(entry.getValue().getProjectCompartmentUUID().toString().equalsIgnoreCase(compartmentNameOrUUID)) {
                targetCompartment = entry.getValue();
                break;
            }
        }
        if(targetCompartment == null) {
            for (Map.Entry<UUID, JIPipeProjectCompartment> entry : project.getCompartments().entrySet()) {
                if(entry.getValue().getName().equalsIgnoreCase(compartmentNameOrUUID)) {
                    targetCompartment = entry.getValue();
                    break;
                }
            }
        }
        if(targetCompartment == null) {
            System.err.println("Compartment " + compartmentNameOrUUID + " not found!");
            JIPipeCLIHelp.showHelp();
            return;
        }

        // Create canvas and render the project
        JIPipeDesktopWorkbench workbench = new JIPipeDesktopDummyWorkbench();
        JIPipeDesktopGraphCanvasUI canvasUI = new JIPipeDesktopGraphCanvasUI(workbench,
                null,
                project.getGraph(),
                targetCompartment.getProjectCompartmentUUID(),
                new JIPipeDedicatedGraphHistoryJournal(project.getGraph()));
        canvasUI.setRenderCursor(false);
        BufferedImage image = canvasUI.createScreenshotPNG();
        try {
            ImageIO.write(image, "PNG", outputFile.toFile());
            System.out.println("Saved image to " + outputFile.toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().halt(0);

    }
}
